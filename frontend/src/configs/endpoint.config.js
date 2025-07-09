export const apiPrefix = 'http://localhost:8080'

const endpointConfig = {
    signIn: apiPrefix + '/auth/login',
    signOut: apiPrefix + '/auth/logout',
    checkSession: apiPrefix + '/auth/session',
    signUp: '/sign-up',
    forgotPassword: '/forgot-password',
    resetPassword: '/reset-password',
}

export default endpointConfig
