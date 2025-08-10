import { useEffect, useRef, useState } from 'react'
import AuthContext from './AuthContext'
import appConfig from '@/configs/app.config'
import { useSessionUser } from '@/store/authStore'
import { apiSignIn, apiSignOut, apiSignUp } from '@/services/AuthService'
import { REDIRECT_URL_KEY } from '@/constants/app.constant'
import { useLocation, useNavigate } from 'react-router'
import { tokenManager } from '@/utils/hooks/tokenManager.jsx'
import { cookieHelpers } from '@/utils/cookiesStorage.js'

function AuthProvider({ children }) {
    const [isInitializing, setIsInitializing] = useState(true)
    const [isLoggingOut, setIsLoggingOut] = useState(false)
    const initializationStarted = useRef(false)
    const redirectProcessed = useRef(false)
    const lastRedirectTime = useRef(0)
    const logoutInProgress = useRef(false)

    const signedIn = useSessionUser((state) => state.session.signedIn)
    const user = useSessionUser((state) => state.user)
    const checkSession = useSessionUser((state) => state.checkSession)
    const forceCheckSession = useSessionUser((state) => state.forceCheckSession)
    const reset = useSessionUser((state) => state.reset)
    const clearSession = useSessionUser((state) => state.clearSession)
    const loginSuccess = useSessionUser((state) => state.loginSuccess)
    const isLoggedOutManually = useSessionUser((state) => state.isLoggedOutManually)
    const setNavigator = useSessionUser((state) => state.setNavigator)

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

    useEffect(() => {
        const handleLoginSuccess = (event) => {
            handleSignIn(event.detail)
        }

        window.addEventListener('loginSuccess', handleLoginSuccess)
        return () => window.removeEventListener('loginSuccess', handleLoginSuccess)
    }, [])

    const isPageRefresh = () => {
        return performance.getEntriesByType('navigation')[0]?.type === 'reload'
    }

    const canRedirect = () => {
        const now = Date.now()
        const timeSinceLastRedirect = now - lastRedirectTime.current
        return timeSinceLastRedirect > 1000 && !isLoggingOut && !logoutInProgress.current
    }

    const performRedirect = (url) => {
        if (canRedirect()) {
            lastRedirectTime.current = Date.now()
            redirectProcessed.current = true
            setTimeout(() => {
                navigate(url, { replace: true })
            }, 50)
        }
    }

    useEffect(() => {
        if (initializationStarted.current || isLoggingOut || logoutInProgress.current) {
            return
        }
        initializationStarted.current = true

        const verifySession = async () => {
            try {
                // 쿠키 헬퍼를 사용하여 토큰 확인
                const cookieToken = cookieHelpers.getAccessToken()
                const userInfo = cookieHelpers.getUserInfo()

                if (cookieToken && userInfo) {
                    tokenManager.setAccessToken(cookieToken)

                    // 토큰 만료 시간 설정
                    const expiryTime = cookieHelpers.getTokenExpiry()
                    if (expiryTime) {
                        tokenManager.setTokenExpiry(expiryTime)
                    }

                    // 토큰 유효성 검증 추가
                    if (tokenManager.isAccessTokenExpired()) {
                        console.warn('Token expired, clearing session')
                        clearSession()
                        cookieHelpers.clearAuthCookies()

                        const currentPath = location.pathname
                        const authRoutes = ['/sign-in', '/sign-up', '/forgot-password']

                        if (!authRoutes.includes(currentPath) && currentPath !== '/' && !redirectProcessed.current) {
                            const redirectUrl = '/sign-in?redirectUrl=' + encodeURIComponent(currentPath)
                            performRedirect(redirectUrl)
                        } else if (currentPath === '/' && !redirectProcessed.current) {
                            performRedirect('/home')
                        }

                        setIsInitializing(false)
                        return
                    }

                    loginSuccess({
                        accessToken: cookieToken,
                        userId: userInfo.userId,
                        userName: userInfo.userName,
                        userSeCd: userInfo.userSeCd,
                        authorities: userInfo.authorities || [],
                    })

                    // 로그인된 상태에서 루트 경로 접근시 /home으로 리디렉트
                    if (location.pathname === '/' && !redirectProcessed.current) {
                        performRedirect('/home')
                    }

                    setIsInitializing(false)
                    return
                }

                const token = tokenManager.getAccessToken()
                const currentPath = location.pathname
                const authRoutes = ['/sign-in', '/sign-up', '/forgot-password']

                // 루트 경로 접근시 /home으로 리디렉트 (인증되지 않은 상태)
                if (currentPath === '/' && !redirectProcessed.current) {
                    performRedirect('/home')
                    setIsInitializing(false)
                    return
                }

                if (token && !tokenManager.isAccessTokenExpired() && !isLoggedOutManually) {
                    if (signedIn && user?.userId) {
                        // 로그인된 상태에서 루트 경로 접근시 /home으로 리디렉트
                        if (currentPath === '/' && !redirectProcessed.current) {
                            performRedirect('/home')
                        }
                        setIsInitializing(false)
                        return
                    }

                    // 페이지 새로고침 시 더 안전한 세션 검증
                    let isValid
                    try {
                        isValid = isPageRefresh() ? await forceCheckSession() : await checkSession()
                    } catch (sessionError) {
                        console.warn('Session check failed:', sessionError)
                        isValid = false
                    }

                    if (isValid) {
                        if (authRoutes.includes(currentPath) && !redirectProcessed.current) {
                            const redirectUrl = getRedirectUrl()
                            if (redirectUrl && redirectUrl !== currentPath) {
                                performRedirect(redirectUrl)
                            } else {
                                performRedirect(appConfig.authenticatedEntryPath)
                            }
                        } else if (currentPath === '/' && !redirectProcessed.current) {
                            performRedirect('/home')
                        }
                    } else {
                        clearSession()
                        cookieHelpers.clearAuthCookies()

                        if (!authRoutes.includes(currentPath) && currentPath !== '/' && !redirectProcessed.current) {
                            const redirectUrl = '/sign-in?redirectUrl=' + encodeURIComponent(currentPath)
                            performRedirect(redirectUrl)
                        } else if (currentPath === '/' && !redirectProcessed.current) {
                            performRedirect('/home')
                        }
                    }
                } else {
                    clearSession()
                    cookieHelpers.clearAuthCookies()

                    if (!authRoutes.includes(currentPath) && currentPath !== '/' && !redirectProcessed.current) {
                        const redirectUrl = '/sign-in?redirectUrl=' + encodeURIComponent(currentPath)
                        performRedirect(redirectUrl)
                    } else if (currentPath === '/' && !redirectProcessed.current) {
                        performRedirect('/home')
                    }
                }
            } catch (error) {
                console.error('Session verification failed:', error)
                clearSession()
                cookieHelpers.clearAuthCookies()

                const currentPath = location.pathname
                const authRoutes = ['/sign-in', '/sign-up', '/forgot-password']

                if (!authRoutes.includes(currentPath) && currentPath !== '/' && !redirectProcessed.current) {
                    const redirectUrl = '/sign-in?redirectUrl=' + encodeURIComponent(currentPath)
                    performRedirect(redirectUrl)
                } else if (currentPath === '/' && !redirectProcessed.current) {
                    performRedirect('/home')
                }
            } finally {
                setIsInitializing(false)
            }
        }

        verifySession()
    }, [location.pathname, signedIn, user?.userId, isLoggedOutManually, checkSession, forceCheckSession, clearSession, loginSuccess, isLoggingOut])

    useEffect(() => {
        const timer = setTimeout(() => {
            redirectProcessed.current = false
        }, 2000)

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
        window.location.replace('/sign-in')
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
                const redirectUrl = getRedirectUrl() || appConfig.authenticatedEntryPath
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
        logoutInProgress.current = true
        setIsLoggingOut(true)

        setTimeout(async () => {
            try {
                tokenManager.removeTokens()
                cookieHelpers.clearAuthCookies()
                reset()
                clearSession()

                apiSignOut().catch(error => {
                    console.error('Sign out API error:', error)
                })

            } catch (error) {
                console.error('Sign out cleanup error:', error)
            }
        }, 0)

        handleSignOut()
    }

    const oAuthSignIn = (callback) => {
        callback({
            onSignIn: handleSignIn,
            redirect: () => {
                const redirectUrl = getRedirectUrl() || appConfig.authenticatedEntryPath
                performRedirect(redirectUrl)
            },
        })
    }

    // 로그아웃 중일 때는 빈 화면
    if (isLoggingOut) {
        return null
    }

    // 초기화 중일 때만 로딩 화면 (로그인 전환 상태 제거)
    if (isInitializing) {
        return (
            <div style={{
                position: 'fixed',
                top: 0,
                left: 0,
                width: '100%',
                height: '100%',
                backgroundColor: 'white',
                zIndex: 9999
            }}>
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