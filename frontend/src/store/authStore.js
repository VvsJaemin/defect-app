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
            checkSession: async () => {
                try {
                    if (!get().isLoggedOutManually) {
                        const response = await axios.get(
                            appConfig.apiPrefix + '/auth/session',
                            { withCredentials: true },
                        )
                        const userAuthority = response.data.userSeCd;
                        console.log(userAuthority);
                        if (response.data && response.data.userId) {
                            set({
                                session: { signedIn: true },
                                user: response.data,
                                authority: userAuthority,
                                isLoggedOutManually: false,
                            })
                            return true
                        } else {
                            set(() => ({ ...initialState, navigator: get().navigator }))
                            return false
                        }
                    }
                } catch (error) {
                    set(() => ({ ...initialState, navigator: get().navigator }))
                    if (error?.response?.status === 401) {
                        const redirectUrl = '/sign-in?redirectUrl=' + encodeURIComponent(window.location.pathname);
                        const navigator = get().navigator;
                        if (navigator) {
                            navigator(redirectUrl); // 함수 자체 호출
                        } else {
                            window.location.href = redirectUrl; // 예비 수단
                        }
                    }
                    return false;
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
