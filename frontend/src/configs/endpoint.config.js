export const apiPrefix = import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_PREFIX


const endpointConfig = {
    signIn: apiPrefix + '/auth/login',
    signOut: apiPrefix + '/auth/logout',
    checkSession: apiPrefix + '/auth/session',
    signUp: '/sign-up',
    forgotPassword: '/forgot-password',
    resetPassword: '/reset-password',
}

export default endpointConfig
