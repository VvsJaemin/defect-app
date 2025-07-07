import { lazy } from 'react'

const authRoute = [
    {
        key: 'signIn',
        path: `/sign-in`,
        component: lazy(() => import('@/views/auth/SignIn/index.js')),
        authority: [],
    },
    {
        key: 'signUp',
        path: `/sign-up`,
        component: lazy(() => import('@/views/auth/SignUp/index.js')),
        authority: [],
    },
]

export default authRoute
