
import axios from 'axios'
import { tokenManager } from '@/utils/hooks/tokenManager.jsx'
import { apiPrefix } from '@/configs/endpoint.config'

const ApiService = axios.create({
    baseURL: apiPrefix,
    withCredentials: true, // httpOnly 쿠키 자동 포함
    headers: {
        'Content-Type': 'application/json'
    }
})

// 요청 인터셉터
ApiService.interceptors.request.use(
    (config) => {
        // 메모리에 있는 Access Token을 헤더에 추가
        const token = tokenManager.getAccessToken()
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }

        // FormData인 경우 Content-Type을 자동으로 설정하도록 헤더 제거
        if (config.data instanceof FormData) {
            delete config.headers['Content-Type']
        }

        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 응답 인터셉터
ApiService.interceptors.response.use(
    (response) => {
        return response
    },
    async (error) => {
        const originalRequest = error.config

        // 401 에러이고 재시도하지 않은 경우
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true

            try {
                // 토큰 갱신 시도
                const refreshed = await tokenManager.refreshToken()

                if (refreshed) {
                    // 새 토큰으로 원래 요청 재시도
                    const newToken = tokenManager.getAccessToken()
                    if (newToken) {
                        originalRequest.headers.Authorization = `Bearer ${newToken}`
                        return ApiService(originalRequest)
                    }
                }
            } catch (refreshError) {
                console.error('토큰 갱신 실패:', refreshError)
            }

            // 갱신 실패 시 로그아웃
            tokenManager.removeTokens()
            window.location.href = '/sign-in'
        }

        return Promise.reject(error)
    }
)

export default ApiService