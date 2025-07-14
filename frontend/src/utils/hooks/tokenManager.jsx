import { cookieHelpers } from '@/utils/cookiesStorage.js'

class TokenManager {
    constructor() {
        this.accessToken = null
        this.tokenExpiry = null
        this.init()
    }

    init() {
        // 쿠키에서 토큰 정보 로드
        this.accessToken = cookieHelpers.getAccessToken()
        this.tokenExpiry = cookieHelpers.getTokenExpiry()
    }

    setAccessToken(token) {
        this.accessToken = token
        if (token) {
            cookieHelpers.setAccessToken(token)
        }
    }

    getAccessToken() {
        if (!this.accessToken) {
            this.accessToken = cookieHelpers.getAccessToken()
        }
        return this.accessToken
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