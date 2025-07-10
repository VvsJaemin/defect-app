const appConfig = {
    apiPrefix: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    authenticatedEntryPath: '/home',
    unAuthenticatedEntryPath: '/sign-in',
    locale: 'en',
    accessTokenPersistStrategy: 'cookies',
    enableMock: false,
    activeNavTranslation: false,
}

export default appConfig
