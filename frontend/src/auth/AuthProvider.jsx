import { useEffect, useRef, useState } from 'react'
import AuthContext from './AuthContext'
import appConfig from '@/configs/app.config'
import { useSessionUser } from '@/store/authStore'
import { apiSignIn, apiSignOut, apiSignUp } from '@/services/AuthService'
import { REDIRECT_URL_KEY } from '@/constants/app.constant'
import { useLocation, useNavigate } from 'react-router'
import { tokenManager } from '@/utils/hooks/tokenManager.jsx'

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

function AuthProvider({ children }) {
    const [isInitializing, setIsInitializing] = useState(true)
    const initializationStarted = useRef(false)
    const redirectProcessed = useRef(false)
    const lastRedirectTime = useRef(0)

    const signedIn = useSessionUser((state) => state.session.signedIn)
    const user = useSessionUser((state) => state.user)
    const checkSession = useSessionUser((state) => state.checkSession)
    const forceCheckSession = useSessionUser((state) => state.forceCheckSession)
    const reset = useSessionUser((state) => state.reset)
    const clearSession = useSessionUser((state) => state.clearSession)
    const loginSuccess = useSessionUser((state) => state.loginSuccess)
    const isLoggedOutManually = useSessionUser(
        (state) => state.isLoggedOutManually,
    )
    const setNavigator = useSessionUser((state) => state.setNavigator)
    const initialized = useSessionUser((state) => state.initialized)

    const authenticated = Boolean(
        signedIn &&
            tokenManager.isAuthenticated() &&
            !tokenManager.isAccessTokenExpired(),
    )

    const navigate = useNavigate()
    const location = useLocation()

    useEffect(() => {
        setNavigator(navigate)
        return () => setNavigator(null)
    }, [setNavigator, navigate])

    // 로그인 성공 이벤트 리스너
    useEffect(() => {
        const handleLoginSuccess = (event) => {
            handleSignIn(event.detail)
        }

        window.addEventListener('loginSuccess', handleLoginSuccess)
        return () =>
            window.removeEventListener('loginSuccess', handleLoginSuccess)
    }, [])

    const isPageRefresh = () => {
        return performance.getEntriesByType('navigation')[0]?.type === 'reload'
    }

    // 리디렉션 방지를 위한 헬퍼 함수
    const canRedirect = () => {
        const now = Date.now()
        const timeSinceLastRedirect = now - lastRedirectTime.current
        return timeSinceLastRedirect > 1000 // 1초 이내 중복 리디렉션 방지
    }

    const performRedirect = (url) => {
        if (canRedirect()) {
            lastRedirectTime.current = Date.now()
            redirectProcessed.current = true
            setTimeout(() => {
                navigate(url, { replace: true })
            }, 100)
        }
    }

    useEffect(() => {
        if (initializationStarted.current) {
            return
        }
        initializationStarted.current = true

        const verifySession = async () => {
            try {
                // 먼저 쿠키에서 직접 토큰 확인
                const cookieToken =
                    getCookieValue('clientAccessToken') ||
                    getCookieValue('accessToken')
                const userInfoStr = getCookieValue('userInfo')

                if (cookieToken && userInfoStr) {
                    try {
                        const userInfo = JSON.parse(
                            decodeURIComponent(userInfoStr),
                        )
                        tokenManager.setAccessToken(cookieToken)

                        // 세션 스토어에 사용자 정보 설정
                        loginSuccess({
                            accessToken: cookieToken,
                            userId: userInfo.userId,
                            userName: userInfo.userName,
                            userSeCd: userInfo.userSeCd,
                            authorities: userInfo.authorities || [],
                        })

                        setIsInitializing(false)
                        return
                    } catch (error) {
                    }
                }

                const token = tokenManager.getAccessToken()
                const currentPath = location.pathname
                const authRoutes = ['/sign-in', '/sign-up', '/forgot-password']

                if (
                    token &&
                    !tokenManager.isAccessTokenExpired() &&
                    !isLoggedOutManually
                ) {
                    if (signedIn && user?.userId) {
                        setIsInitializing(false)
                        return
                    }

                    const isValid = isPageRefresh()
                        ? await forceCheckSession()
                        : await checkSession()

                    if (isValid) {
                        // 인증 성공 시 로그인 페이지에서 벗어나기
                        if (
                            authRoutes.includes(currentPath) &&
                            !redirectProcessed.current
                        ) {
                            const redirectUrl = getRedirectUrl()
                            if (redirectUrl && redirectUrl !== currentPath) {
                                performRedirect(redirectUrl)
                            } else {
                                performRedirect(
                                    appConfig.authenticatedEntryPath,
                                )
                            }
                        }
                    } else {
                        clearSession()
                        if (
                            !authRoutes.includes(currentPath) &&
                            !redirectProcessed.current
                        ) {
                            const redirectUrl =
                                '/sign-in?redirectUrl=' +
                                encodeURIComponent(currentPath)
                            performRedirect(redirectUrl)
                        }
                    }
                } else {
                    clearSession()
                    if (
                        !authRoutes.includes(currentPath) &&
                        !redirectProcessed.current
                    ) {
                        const redirectUrl =
                            '/sign-in?redirectUrl=' +
                            encodeURIComponent(currentPath)
                        performRedirect(redirectUrl)
                    }
                }
            } catch (error) {
                clearSession()
                const currentPath = location.pathname
                const authRoutes = ['/sign-in', '/sign-up', '/forgot-password']

                if (
                    !authRoutes.includes(currentPath) &&
                    !redirectProcessed.current
                ) {
                    const redirectUrl =
                        '/sign-in?redirectUrl=' +
                        encodeURIComponent(currentPath)
                    performRedirect(redirectUrl)
                }
            } finally {
                setIsInitializing(false)
            }
        }

        verifySession()
    }, [])

    // 경로 변경 시 리디렉션 플래그 리셋
    useEffect(() => {
        const timer = setTimeout(() => {
            redirectProcessed.current = false
        }, 2000) // 2초 후 리셋

        return () => clearTimeout(timer)
    }, [location.pathname])

    const getRedirectUrl = () => {
        const search = window.location.search
        const params = new URLSearchParams(search)
        return params.get(REDIRECT_URL_KEY)
    }

    const handleSignIn = (userData) => {
        if (!userData || !userData.userId) {
            return
        }
        loginSuccess(userData)
    }

    const handleSignOut = () => {
        tokenManager.removeTokens()
        reset()
        redirectProcessed.current = false
        lastRedirectTime.current = 0
    }

    const signIn = async (values) => {
        try {
            const resp = await apiSignIn(values)
            if (resp && resp.result === 'success') {
                return { status: 'success', message: '' }
            }
            return { status: 'failed', message: 'Unable to sign in' }
        } catch (error) {
            return {
                status: 'failed',
                message: error?.response?.data?.message || 'Login failed',
            }
        }
    }

    const signUp = async (values) => {
        try {
            const resp = await apiSignUp(values)
            if (resp && resp.userId) {
                handleSignIn(resp)
                const redirectUrl =
                    getRedirectUrl() || appConfig.authenticatedEntryPath
                performRedirect(redirectUrl)
                return { status: 'success', message: '' }
            }
            return {
                status: 'failed',
                message: 'Unable to sign up: No user data received',
            }
        } catch (error) {
            return {
                status: 'failed',
                message: error?.response?.data?.message || 'Sign-up failed',
            }
        }
    }

    const signOut = async () => {
        try {
            await apiSignOut()
        } catch (error) {

        } finally {
            handleSignOut()
            navigate(appConfig.unAuthenticatedEntryPath || '/sign-in')
        }
    }

    const oAuthSignIn = (callback) => {
        callback({
            onSignIn: handleSignIn,
            redirect: () => {
                const redirectUrl =
                    getRedirectUrl() || appConfig.authenticatedEntryPath
                performRedirect(redirectUrl)
            },
        })
    }

    if (isInitializing) {
        return (
            <div className="flex items-center justify-center h-screen">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
                    <p className="mt-2 text-sm text-gray-600">로딩 중...</p>
                </div>
            </div>
        )
    }

    return (
        <AuthContext.Provider
            value={{
                authenticated,
                user,
                signIn,
                signUp,
                signOut,
                oAuthSignIn,
            }}
        >
            {children}
        </AuthContext.Provider>
    )
}

export default AuthProvider