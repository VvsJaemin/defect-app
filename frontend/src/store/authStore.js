
import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import axios from 'axios'
import appConfig from '@/configs/app.config.js'

// 내부 상태
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
            reset: () =>
                set(() => ({
                    ...initialState,
                    isLoggedOutManually: true,
                    navigator: get().navigator // navigator는 유지
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
                    navigator: get().navigator
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
                    navigator: get().navigator
                }))
            },
            checkSession: async () => {
                try {
                    const currentState = get()

                    if (!currentState.isLoggedOutManually) {
                        const response = await axios.get(
                            appConfig.apiPrefix + '/auth/session',
                            {
                                withCredentials: true,
                                timeout: 10000 // 10초 타임아웃 추가
                            },
                        )
                        const userAuthority = response.data.userSeCd;
                        if (response.data && response.data.userId) {
                            set({
                                session: { signedIn: true },
                                user: response.data,
                                authority: userAuthority,
                                isLoggedOutManually: false,
                            })
                            return true
                        } else {
                            // 세션 데이터가 유효하지 않은 경우 완전 초기화
                            get().clearSession()
                            return false
                        }
                    }
                    return false
                } catch (error) {
                    // 403이나 401 오류인 경우 완전히 세션 초기화
                    if (error?.response?.status === 403 || error?.response?.status === 401) {
                        get().clearSession()

                        // 현재 경로가 로그인 페이지가 아닌 경우에만 리다이렉트
                        const currentPath = window.location.pathname
                        const authRoutes = ['/sign-in']

                        if (!authRoutes.includes(currentPath)) {
                            const redirectUrl = '/sign-in?redirectUrl=' + encodeURIComponent(currentPath)
                            const navigator = get().navigator
                            if (navigator) {
                                navigator(redirectUrl)
                            } else {
                                window.location.href = redirectUrl
                            }
                        }
                        return false
                    }

                    // // 네트워크 오류나 서버 오류인 경우
                    // if (!error.response || error.response.status >= 500) {
                    //     // 서버 오류인 경우도 세션 완전 초기화
                    //     get().clearSession()
                    //     throw error
                    // }
                    //
                    // // 기타 오류인 경우 세션 완전 초기화
                    // get().clearSession()
                    // return false
                }
            },
        }),
        {
            name: 'sessionUser',
            storage: createJSONStorage(() => localStorage),
        },
    ),
)

export const useToken = () => ({
    setToken: () => {},
    token: null,
})