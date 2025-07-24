import { create } from 'zustand'
import { tokenManager } from '@/utils/hooks/tokenManager.jsx'
import ApiService from '@/services/ApiService.js'

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

const useSessionUser = create((set, get) => ({
    session: { signedIn: false, token: null },
    user: {
        userId: '',
        userName: '',
        userSeCd: '',
        authorities: [],
    },
    isLoggedOutManually: false,
    initialized: false,
    navigator: null,

    setNavigator: (navigate) => set({ navigator: navigate }),

    loginSuccess: (userData) => {
        // 로그인 성공 시 토큰 저장 및 사용자 정보 설정
        if (userData.accessToken) {
            tokenManager.setAccessToken(userData.accessToken)
        }

        set({
            session: { signedIn: true, token: userData.accessToken },
            user: {
                userId: userData.userId,
                userName: userData.userName,
                userSeCd: userData.userSeCd,
                authorities: userData.authorities || [],
            },
            isLoggedOutManually: false,
            initialized: true
        })
    },

    checkSession: async () => {
        try {
            // 먼저 쿠키에서 토큰 확인
            const accessToken = getCookieValue('accessToken')
            const userInfoStr = getCookieValue('userInfo')

            if (accessToken && userInfoStr) {
                try {
                    const userInfo = JSON.parse(decodeURIComponent(userInfoStr))

                    // 토큰 매니저에 토큰 설정
                    tokenManager.setAccessToken(accessToken)

                    set({
                        session: { signedIn: true, token: accessToken },
                        user: {
                            userId: userInfo.userId,
                            userName: userInfo.userName,
                            userSeCd: userInfo.userSeCd,
                            authorities: userInfo.authorities || [],
                        },
                        isLoggedOutManually: false,
                        initialized: true
                    })

                    return true
                } catch (error) {
                    console.error('사용자 정보 파싱 오류:', error)
                }
            }

            // 쿠키에 정보가 없으면 서버에서 확인
            const response = await ApiService.get('/auth/me')

            if (response.data && response.data.userId) {
                set({
                    session: { signedIn: true, token: accessToken },
                    user: {
                        userId: response.data.userId,
                        userName: response.data.userName,
                        userSeCd: response.data.userSeCd,
                        authorities: response.data.authorities || [],
                    },
                    isLoggedOutManually: false,
                    initialized: true
                })
                return true
            }

            return false
        } catch (error) {
            console.error('세션 확인 오류:', error)
            return false
        }
    },

    forceCheckSession: async () => {
        try {
            const response = await ApiService.get('/auth/me')

            if (response.data && response.data.userId) {
                const accessToken = getCookieValue('accessToken')

                if (accessToken) {
                    tokenManager.setAccessToken(accessToken)
                }

                set({
                    session: { signedIn: true, token: accessToken },
                    user: {
                        userId: response.data.userId,
                        userName: response.data.userName,
                        userSeCd: response.data.userSeCd,
                        authorities: response.data.authorities || [],
                    },
                    isLoggedOutManually: false,
                    initialized: true
                })
                return true
            }

            return false
        } catch (error) {
            console.error('강제 세션 확인 오류:', error)
            return false
        }
    },

    clearSession: () => {
        tokenManager.removeTokens()
        set({
            session: { signedIn: false, token: null },
            user: {
                userId: '',
                userName: '',
                userSeCd: '',
                authorities: [],
            },
            isLoggedOutManually: true,
            initialized: true
        })
    },

    reset: () => {
        tokenManager.removeTokens()
        set({
            session: { signedIn: false, token: null },
            user: {
                userId: '',
                userName: '',
                userSeCd: '',
                authorities: [],
            },
            isLoggedOutManually: false,
            initialized: false
        })
    },
}))

export { useSessionUser }