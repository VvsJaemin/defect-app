import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import axios from 'axios'
import appConfig from '@/configs/app.config.js'

// 내부 상태 (로컬스토리지에 저장되지 않음)
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
    navigator: null, // navigate 함수 보관
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

            // 로그인 성공 시 호출할 함수
            loginSuccess: (userData) => {
                set(() => ({
                    session: { signedIn: true },
                    user: userData,
                    isLoggedOutManually: false, // 로그인 성공 시 수동 로그아웃 플래그 초기화
                }))
            },

            reset: () =>
                set(() => ({
                    ...initialState,
                    isLoggedOutManually: true,
                    navigator: get().navigator, // navigator는 유지
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
                // 로컬스토리지에서 완전히 제거
                localStorage.removeItem('sessionUser')
                // 상태도 초기화
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
                    const currentState = get()
                    const currentPath = window.location.pathname
                    const isLoginPage = currentPath === '/sign-in'

                    // 로그인 페이지에서는 세션 체크를 하지 않음
                    if (isLoginPage) {
                        return false
                    }

                    // 수동 로그아웃 상태가 아니거나, 로그인 페이지가 아닌 경우 세션 체크 수행
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

                    // 네트워크 오류 등의 경우 false 반환
                    return false
                }
            },

            // 사용자 정보 업데이트 함수
            updateUserInfo: (userInfo) => {
                set((state) => ({
                    user: { ...state.user, ...userInfo },
                }))
            },

            // 세션 상태 확인 함수
            isAuthenticated: () => {
                const state = get()
                return state.session.signedIn && state.user.userId !== ''
            },
        }),
        {
            name: 'sessionUser',
            storage: createJSONStorage(() => localStorage),
            // 최소한의 정보만 저장 - isLoggedOutManually는 제외
            partialize: (state) => ({
                session: state.session,
                // 보안상 사용자 정보는 저장하지 않음 - 세션 체크를 통해 서버에서 가져옴
            }),
            // 스토리지에서 복원 시 초기화 작업
            onRehydrateStorage: () => (state) => {
                if (state) {
                    // 복원 시 isLoggedOutManually는 항상 false로 설정
                    state.isLoggedOutManually = false
                    // 복원 시 사용자 정보는 빈 상태로 설정
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