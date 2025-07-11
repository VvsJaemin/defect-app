
import { useEffect } from 'react'
import AuthContext from './AuthContext'
import appConfig from '@/configs/app.config'
import { useSessionUser } from '@/store/authStore'
import { apiSignIn, apiSignOut, apiSignUp } from '@/services/AuthService'
import { REDIRECT_URL_KEY } from '@/constants/app.constant'
import { useNavigate } from 'react-router'

function AuthProvider({ children }) {
    const signedIn = useSessionUser((state) => state.session.signedIn)
    const user = useSessionUser((state) => state.user)
    const setUser = useSessionUser((state) => state.setUser)
    const setSessionSignedIn = useSessionUser(
        (state) => state.setSessionSignedIn,
    )
    const checkSession = useSessionUser((state) => state.checkSession)
    const reset = useSessionUser((state) => state.reset)
    const clearSession = useSessionUser((state) => state.clearSession)
    const isLoggedOutManually = useSessionUser(
        (state) => state.isLoggedOutManually,
    )
    const setNavigator = useSessionUser((state) => state.setNavigator)

    const authenticated = Boolean(signedIn)

    const navigate = useNavigate()

    // zustand store에 네비게이션 함수 등록
    useEffect(() => {
        setNavigator(navigate)
        return () => setNavigator(null) // cleanup
    }, [setNavigator, navigate])

    // 앱 시작 시 세션 확인 - 새로고침 시에도 실행됨
    useEffect(() => {
        const verifySession = async () => {
            try {
                // 로그인 상태가 true인 경우에만 세션 확인
                if (signedIn && !isLoggedOutManually) {
                    const isValid = await checkSession()

                    if (!isValid) {
                        // 현재 경로가 로그인 페이지가 아닌 경우에만 리다이렉트
                        const currentPath = window.location.pathname
                        const authRoutes = ['/sign-in']

                        if (!authRoutes.includes(currentPath)) {
                            const redirectUrl = '/sign-in?redirectUrl=' + encodeURIComponent(currentPath)
                            navigate(redirectUrl)
                        }
                    }
                }
                // isLoggedOutManually가 true이면서 signedIn이 true인 경우 완전 초기화
                else if (signedIn && isLoggedOutManually) {
                    clearSession()
                }
            } catch (error) {
                // 세션 확인 실패 시 상태 완전 초기화
                clearSession()

                // 현재 경로가 로그인 페이지가 아닌 경우 리다이렉트
                const currentPath = window.location.pathname
                const authRoutes = ['/sign-in']

                if (!authRoutes.includes(currentPath)) {
                    const redirectUrl = '/sign-in?redirectUrl=' + encodeURIComponent(currentPath)
                    navigate(redirectUrl)
                }
            }
        }

        verifySession()
    }, [checkSession, signedIn, isLoggedOutManually, navigate, clearSession])

    const redirect = () => {
        const search = window.location.search
        const params = new URLSearchParams(search)
        const redirectUrl =
            params.get(REDIRECT_URL_KEY) || appConfig.authenticatedEntryPath

        navigate(redirectUrl)
    }

    const handleSignIn = (user) => {
        if (!user || !user.userId) {
            console.error('Invalid user data:', user)
            return
        }
        setSessionSignedIn(true)
        setUser(user)
    }

    const handleSignOut = () => {
        reset()
    }

    const signIn = async (values) => {
        try {
            const resp = await apiSignIn(values)
            if (resp && resp.userId) {
                handleSignIn(resp)
                redirect()
                return {
                    status: 'success',
                    message: '',
                }
            }
            return {
                status: 'failed',
                message: 'Unable to sign in: No user data received',
            }
        } catch (error) {
            return {
                status: 'failed',
                message: error?.response?.data,
            }
        }
    }

    const signUp = async (values) => {
        try {
            const resp = await apiSignUp(values)
            if (resp && resp.userId) {
                handleSignIn(resp)
                redirect()
                return {
                    status: 'success',
                    message: '',
                }
            }
            return {
                status: 'failed',
                message: 'Unable to sign up: No user data received',
            }
        } catch (error) {
            console.error('Sign-up error:', error)
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
            console.error('Sign-out error:', error)
        } finally {
            handleSignOut()
            navigate(appConfig.unAuthenticatedEntryPath || '/')
        }
    }

    const oAuthSignIn = (callback) => {
        callback({
            onSignIn: handleSignIn,
            redirect,
        })
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