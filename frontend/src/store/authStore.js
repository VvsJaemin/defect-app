import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import axios from 'axios'
import appConfig from '@/configs/app.config.js'

const initialState = {
    session: {
        signedIn: false,
    },
    user: {
        userId: '',
        userName: '',
        userSeCd: '',
        lastLoginAt: '',
        firstRegDtm: '',
    },
    isLoggedOutManually: false, // 추가
}

export const useSessionUser = create()(
    persist(
        (set) => ({
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
                    user: {
                        ...state.user,
                        ...payload,
                    },
                })),
            reset: () =>
                set(() => ({
                    ...initialState,
                    isLoggedOutManually: true,
                })),
            checkSession: async () => {
                try {
                    if (initialState.isLoggedOutManually) {
                        const response = await axios.get(
                            appConfig.apiPrefix + '/auth/session',
                            {
                                withCredentials: true,
                            },
                        )

                        if (response.data && response.data.userId) {
                            set({
                                session: { signedIn: true },
                                user: response.data,
                                isLoggedOutManually: false, // 로그인 성공 시 상태 초기화
                            })
                            return true
                        } else {
                            set(() => ({ ...initialState }))
                            return false
                        }
                    }
                } catch (error) {
                    if (error?.response?.status === 401) {
                        console.log('Session expired, not signed in.')
                    } else {
                        console.error('Session check failed:', error)
                    }

                    set(() => ({ ...initialState }))
                    return false
                }
            },
        }),
        {
            name: 'sessionUser',
            storage: createJSONStorage(() => sessionStorage), // localStorage 대신 sessionStorage 사용
        },
    ),
)

// useToken 제거 (JSESSIONID 기반 인증에서는 불필요)
// 필요 시 호환성을 위해 빈 객체 반환
export const useToken = () => ({
    setToken: () => {},
    token: null,
})
