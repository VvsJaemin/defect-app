import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import axios from 'axios'
import appConfig from '@/configs/app.config.js'

const initialState = {
    session: { signedIn: false },
    user: {
        userId: '',
        userName: '',
        userSeCd: '',
        lastLoginAt: '',
        firstRegDtm: '',
    },
    isLoggedOutManually: false,
    navigator: null,
}

export const useSessionUser = create()(
    persist(
        (set, get) => ({
            ...initialState,

            setSessionSignedIn: (payload) =>
                set((state) => ({
                    session: {
                        ...state.session,
                        signedIn: payload,
                    },
                })),

            setUser: (payload) =>
                set((state) => ({
                    user: { ...state.user, ...payload },
                })),

            setNavigator: (navigator) => set(() => ({ navigator })),

            loginSuccess: (userData) => {
                set(() => ({
                    session: { signedIn: true },
                    user: userData,
                    isLoggedOutManually: false,
                }))
            },

            reset: () =>
                set(() => ({
                    ...initialState,
                    isLoggedOutManually: true,
                    navigator: get().navigator,
                })),

            forceReset: () => {
                set(() => ({
                    session: { signedIn: false },
                    user: {
                        userId: '',
                        userName: '',
                        userSeCd: '',
                        lastLoginAt: '',
                        firstRegDtm: '',
                    },
                    isLoggedOutManually: false,
                    navigator: get().navigator,
                }))
            },

            clearSession: () => {
                localStorage.removeItem('sessionUser')
                set(() => ({
                    session: { signedIn: false },
                    user: {
                        userId: '',
                        userName: '',
                        userSeCd: '',
                        lastLoginAt: '',
                        firstRegDtm: '',
                    },
                    isLoggedOutManually: false,
                    navigator: get().navigator,
                }))
            },

            checkSession: async () => {
                try {
                    const currentPath = window.location.pathname
                    const isLoginPage = currentPath === '/sign-in'

                    if (isLoginPage) {
                        return false
                    }

                    const response = await axios.get(
                        appConfig.apiPrefix + '/auth/session',
                        {
                            withCredentials: true,
                            timeout: 10000,
                        },
                    )

                    const userAuthority = response.data.userSeCd
                    if (response.data && response.data.userId) {
                        set({
                            session: { signedIn: true },
                            user: response.data,
                            authority: userAuthority,
                            isLoggedOutManually: false,
                        })
                        return true
                    } else {
                        get().clearSession()
                        return false
                    }
                } catch (error) {
                    console.error('세션 체크 에러:', error)

                    if (
                        error?.response?.status === 403 ||
                        error?.response?.status === 401
                    ) {
                        get().clearSession()

                        const currentPath = window.location.pathname
                        const authRoutes = ['/sign-in']

                        if (!authRoutes.includes(currentPath)) {
                            const redirectUrl =
                                '/sign-in?redirectUrl=' +
                                encodeURIComponent(currentPath)
                            const navigator = get().navigator
                            if (navigator) {
                                navigator(redirectUrl)
                            } else {
                                window.location.href = redirectUrl
                            }
                        }
                        return false
                    }

                    return false
                }
            },

            updateUserInfo: (userInfo) => {
                set((state) => ({
                    user: { ...state.user, ...userInfo },
                }))
            },

            isAuthenticated: () => {
                const state = get()
                return state.session.signedIn && state.user.userId !== ''
            },
        }),
        {
            name: 'sessionUser',
            storage: createJSONStorage(() => localStorage),
            partialize: (state) => ({
                session: state.session,
            }),
            onRehydrateStorage: () => (state) => {
                if (state) {
                    state.isLoggedOutManually = false
                    state.user = {
                        userId: '',
                        userName: '',
                        userSeCd: '',
                        lastLoginAt: '',
                        firstRegDtm: '',
                    }
                }
            },
        },
    ),
)

export const useToken = () => ({
    setToken: () => {},
    token: null,
})