import { jwtDecode } from 'jwt-decode'
import Cookies from 'js-cookie'

// 비-httpOnly 쿠키 옵션 (클라이언트에서 읽기 가능한 메타데이터용)
const CLIENT_COOKIE_OPTIONS = {
    secure: process.env.NODE_ENV === 'production', // 프로덕션에서만 HTTPS 필요
    sameSite: 'lax', // CSRF 공격 방지
    expires: 1, // 1일 후 만료
}

// 쿠키에서 값 가져오기 (httpOnly 포함)
const getCookieValue = (name) => {
    const cookies = document.cookie.split(';')
    for (let cookie of cookies) {
        const [cookieName, cookieValue] = cookie.trim().split('=')
        if (cookieName === name) {
            return cookieValue
        }
    }
    return null
}

export const tokenManager = {
    // Access Token은 메모리에 저장 (보안상 더 안전)
    _accessToken: null,
    _isInitialized: false,

    setAccessToken(token) {
        if (token) {
            this._accessToken = token
            this._isInitialized = true

            // 토큰 만료 시간만 클라이언트 쿠키에 저장 (httpOnly가 아님)
            try {
                const decodedToken = jwtDecode(token)
                const expirationTime = decodedToken.exp * 1000 // 밀리초로 변환
                Cookies.set(
                    'tokenExpiry',
                    expirationTime.toString(),
                    CLIENT_COOKIE_OPTIONS,
                )

                // 사용자 정보도 저장 (토큰 디코딩 없이 사용하기 위해)
                const userInfo = {
                    userId: decodedToken.sub || decodedToken.userId,
                    userName: decodedToken.userName,
                    userSeCd: decodedToken.userSeCd,
                    authorities: decodedToken.authorities || [],
                }
                Cookies.set(
                    'userInfo',
                    JSON.stringify(userInfo),
                    CLIENT_COOKIE_OPTIONS,
                )
            } catch (error) {
                console.error('토큰 디코딩 오류:', error)
            }
        }
    },

    getAccessToken() {
        // 먼저 메모리에서 확인
        if (this._accessToken) {
            return this._accessToken
        }

        // 메모리에 없으면 쿠키에서 가져오기 (클라이언트 읽기 가능한 쿠키 우선)
        let token = getCookieValue('clientAccessToken')
        if (!token) {
            token = getCookieValue('accessToken')
        }

        if (token) {
            this._accessToken = token
            return token
        }

        return null
    },


    setRefreshToken(token) {
        // Refresh Token은 서버에서 httpOnly 쿠키로 설정
        // 클라이언트에서는 설정하지 않음
        if (token) {
            console.log(
                'Refresh token will be set as httpOnly cookie by server',
            )
        }
    },

    getRefreshToken() {
        // httpOnly 쿠키는 클라이언트에서 접근 불가
        // 서버에서 자동으로 처리
        return null
    },

    removeTokens() {
        this._accessToken = null
        this._isInitialized = false
        Cookies.remove('tokenExpiry')
        Cookies.remove('userInfo')
        // httpOnly 쿠키 삭제는 서버 로그아웃 API에서 처리
    },

    isAuthenticated() {
        const token = this.getAccessToken()
        return token && !this.isAccessTokenExpired()
    },

    isAccessTokenExpired() {
        const token = this.getAccessToken()

        if (!token) {
            // 메모리에 토큰이 없으면 만료 시간 확인
            const expiryTime = Cookies.get('tokenExpiry')
            if (!expiryTime) return true

            const currentTime = Date.now()
            return currentTime >= parseInt(expiryTime)
        }

        try {
            const decodedToken = jwtDecode(token)
            const currentTime = Date.now() / 1000
            // 만료 5분 전에 미리 만료 처리 (토큰 갱신 여유 시간)
            return decodedToken.exp < currentTime + 300
        } catch (error) {
            console.error('토큰 디코딩 오류:', error)
            return true
        }
    },

    isRefreshTokenExpired() {
        // 서버에서 확인해야 함
        return false
    },

    getUserFromToken() {
        // 먼저 메모리의 토큰에서 시도
        const token = this.getAccessToken()
        if (token) {
            try {
                const decodedToken = jwtDecode(token)
                return {
                    userId: decodedToken.sub || decodedToken.userId,
                    userName: decodedToken.userName,
                    userSeCd: decodedToken.userSeCd,
                    authorities: decodedToken.authorities || [],
                }
            } catch (error) {
                console.error('토큰에서 사용자 정보 추출 오류:', error)
            }
        }

        // 토큰이 없으면 쿠키에서 사용자 정보 가져오기
        const userInfoStr = getCookieValue('userInfo')
        if (userInfoStr) {
            try {
                return JSON.parse(decodeURIComponent(userInfoStr))
            } catch (error) {
                console.error('사용자 정보 쿠키 파싱 오류:', error)
            }
        }

        return null
    },

    // 서버에서 토큰 초기화 (페이지 로드 시)
    async initializeFromServer() {
        if (this._isInitialized) {
            return this._accessToken !== null
        }

        try {
            // 서버에서 토큰 상태 확인 및 새 토큰 발급
            const response = await fetch('/auth/check-token', {
                method: 'GET',
                credentials: 'include', // httpOnly 쿠키 포함
                headers: {
                    'Content-Type': 'application/json',
                },
            })

            if (response.ok) {
                const data = await response.json()
                if (data.valid) {
                    // 쿠키에서 토큰 가져오기
                    const accessToken = getCookieValue('accessToken')
                    if (accessToken) {
                        this.setAccessToken(accessToken)
                        return true
                    }
                }
            }

            // 토큰이 없거나 만료된 경우 정리
            this.removeTokens()
            return false
        } catch (error) {
            console.error('서버에서 토큰 초기화 오류:', error)
            this.removeTokens()
            return false
        }
    },

    // 토큰 갱신 (서버에서 자동 처리)
    async refreshToken() {
        try {
            const response = await fetch('/auth/refresh', {
                method: 'POST',
                credentials: 'include', // httpOnly 쿠키 포함
                headers: {
                    'Content-Type': 'application/json',
                },
            })

            if (response.ok) {
                const data = await response.json()
                if (data.result === 'success') {
                    // 새로운 토큰을 쿠키에서 가져오기
                    const newAccessToken = getCookieValue('accessToken')
                    if (newAccessToken) {
                        this.setAccessToken(newAccessToken)
                        return true
                    }
                }
            }

            return false
        } catch (error) {
            console.error('토큰 갱신 오류:', error)
            return false
        }
    },

    // 메모리에 토큰이 있는지 확인
    hasTokenInMemory() {
        return this._accessToken !== null
    },

    // 초기화 상태 확인
    isInitialized() {
        return this._isInitialized
    },

    // 쿠키에서 토큰 초기화 (로그인 후 사용)
    initializeFromCookie() {
        const token = getCookieValue('accessToken')
        if (token) {
            this.setAccessToken(token)
            return true
        }
        return false
    },
}