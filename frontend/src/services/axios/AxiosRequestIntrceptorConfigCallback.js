import { useSessionUser } from '@/store/authStore'
import { tokenManager } from '@/utils/hooks/tokenManager.jsx'

const unauthorizedCode = [401, 419, 440, 403]

const AxiosResponseInterceptorErrorCallback = (error) => {
    const { response } = error

    if (response && unauthorizedCode.includes(response.status)) {
        // 토큰 제거
        tokenManager.removeTokens()

        // 세션 상태 초기화
        const { clearSession } = useSessionUser.getState()
        clearSession()

        // 로그인 페이지로 리다이렉트
        const currentPath = window.location.pathname
        const authRoutes = ['/sign-in', '/sign-up']

        if (!authRoutes.includes(currentPath)) {
            const redirectUrl = '/sign-in?redirectUrl=' + encodeURIComponent(currentPath)
            window.location.href = redirectUrl
        }
    }

    return Promise.reject(error)
}

export default AxiosResponseInterceptorErrorCallback