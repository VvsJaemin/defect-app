import { cookieHelpers } from '@/utils/cookiesStorage.js'

class TokenManager {
    constructor() {
        this.accessToken = null
        this.tokenExpiry = null
        this.isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
        this.init()

    }


    init() {
        // 모바일에서는 초기화 지연
        if (this.isMobile) {
            setTimeout(() => {
                this.accessToken = cookieHelpers.getAccessToken()
                this.tokenExpiry = cookieHelpers.getTokenExpiry()

                // 모바일에서 alert로 확인
                if (this.accessToken) {
                    alert(`모바일 토큰 초기화 성공: ${this.accessToken.substring(0, 20)}...`)
                } else {
                    alert('모바일 토큰 초기화 실패: 토큰 없음')
                }
            }, 100)
        } else {
            this.accessToken = cookieHelpers.getAccessToken()
            this.tokenExpiry = cookieHelpers.getTokenExpiry()
        }
    }

    getAccessToken() {
        if (!this.accessToken) {
            this.accessToken = cookieHelpers.getAccessToken()

            // 모바일에서 토큰 조회 결과 확인
            if (this.isMobile) {
                if (this.accessToken) {
                    alert(`모바일 토큰 조회 성공: ${this.accessToken.substring(0, 20)}...`)
                } else {
                    alert('모바일 토큰 조회 실패: 토큰 없음')
                    // 쿠키 전체 확인
                    alert(`전체 쿠키: ${document.cookie}`)
                }
            }
        }
        return this.accessToken
    }

    setAccessToken(token) {
        this.accessToken = token
        if (token) {
            cookieHelpers.setAccessToken(token)

            // 모바일에서 토큰 설정 확인
            if (this.isMobile) {
                alert(`모바일 토큰 설정 완료: ${token.substring(0, 20)}...`)
            }
        }
    }

    setTokenExpiry(expiryTime) {
        this.tokenExpiry = expiryTime
        cookieHelpers.setTokenExpiry(expiryTime)
    }

    getTokenExpiry() {
        if (!this.tokenExpiry) {
            this.tokenExpiry = cookieHelpers.getTokenExpiry()
        }
        return this.tokenExpiry
    }

    isAccessTokenExpired() {
        const token = this.getAccessToken()
        const expiry = this.getTokenExpiry()

        if (!token || !expiry) {
            return true
        }

        return Date.now() > expiry
    }

    isAuthenticated() {
        const token = this.getAccessToken()
        return Boolean(token && !this.isAccessTokenExpired())
    }

    removeTokens() {
        this.accessToken = null
        this.tokenExpiry = null
        cookieHelpers.clearAuthCookies()
    }

    async refreshToken() {
        // 서버에서 리프레시 토큰을 통해 새 토큰 요청
        try {
            const response = await fetch('/auth/refresh', {
                method: 'POST',
                credentials: 'include', // httpOnly 쿠키 포함
                headers: {
                    'Content-Type': 'application/json'
                }
            })

            if (response.ok) {
                const data = await response.json()
                if (data.accessToken) {
                    this.setAccessToken(data.accessToken)
                    if (data.expiryTime) {
                        this.setTokenExpiry(data.expiryTime)
                    }
                    return true
                }
            }
        } catch (error) {
            console.error('토큰 갱신 실패:', error)
        }

        return false
    }
}

export const tokenManager = new TokenManager()