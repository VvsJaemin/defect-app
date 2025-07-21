import { lazy } from 'react'
import authRoute from './authRoute'
import othersRoute from './othersRoute'
import { CU, DM, DP, MG, QA } from '@/constants/roles.constant.js'

export const publicRoutes = [...authRoute]

export const protectedRoutes = [
    {
        key: 'home',
        path: '/home',
        component: lazy(() => import('@/views/Home')),
        authority: [],
    },
    /** Example purpose only, please remove */
    {
        key: 'userManagement',
        path: '/user-management',
        component: lazy(() => import('@/views/concepts/users/UserList/UserList.jsx')),
        authority: [MG],
    },
    {
        key: 'userDetails',
        path: '/user-management/details/:userId',
        component: lazy(() => import('@/views/concepts/users/UserDetails/UserDetails.jsx')),
        authority: [MG],
        meta: {
            pageContainerType: 'contained',
        },
    },
    {
        key: 'userInfoUpdate',
        path: '/user-management/update/:userId',
        component: lazy(() => import('@/views/concepts/users/UserEdit/UserEdit.jsx')),
        authority: [MG,CU,DM,DP,QA],
        meta: {
            pageContainerType: 'contained',
        },
    },
    {
        key: 'userInfoCreate',
        path: '/user-management/create',
        component: lazy(() => import('@/views/concepts/users/UserCreate')),
        authority: [MG],
        meta: {
            pageContainerType: 'contained',
        },
    },
    // // 별도의 독립된 탭으로 사용자 정보 수정 추가
    // {
    //     key: 'userInfoUpdate',
    //     path: '/user-management/update/:userId',
    //     component: lazy(() => import('@/views/concepts/users/UserEdit/UserEdit.jsx')),
    //     authority: [MG],
    // },
    {
        key: 'projectManagement',
        path: '/project-management',
        component: lazy(() => import('@/views/concepts/projects/ProjectList')),
        authority: [],

    },
    {
        key: 'projectCreate',
        path: '/project-management/create',
        component: lazy(() => import('@/views/concepts/projects/ProjectCreate')),
        authority: [],
    },
    {
        key: 'projectDetails',
        path: '/project-management/details/:projectId',
        component: lazy(() => import('@/views/concepts/projects/ProjectDetails')),
        authority: [],
        meta: {
            pageContainerType: 'contained',
        },
    },
    {
        key: 'projectUpdate',
        path: '/project-management/update/:projectId',
        component: lazy(() => import('@/views/concepts/projects/ProjectEdit')),
        authority: [],
        meta: {
            pageContainerType: 'contained',
        },
    },
    {
        key: 'defectManagement',
        path: '/defect-management',
        component: lazy(() => import('@/views/concepts/defects/DefectList')),
        authority: [],
    },
    // 새로 추가되는 결함 관리 서브메뉴 라우트들
    {
        key: 'defectManagement.assigned',
        path: '/defect-management/assigned',
        component: lazy(() => import('@/views/concepts/defects/DefectList')),
        authority: [MG,CU,DM,DP,QA],
    },
    {
        key: 'defectManagement.inProgress',
        path: '/defect-management/in-progress',
        component: lazy(() => import('@/views/concepts/defects/DefectList')),
        authority: [MG,CU,DM,DP,QA],
    },
    {
        key: 'defectManagement.completed',
        path: '/defect-management/completed',
        component: lazy(() => import('@/views/concepts/defects/DefectList')),
        authority: [MG,CU,DM,DP,QA],
    },
    {
        key: 'defectManagement.todo',
        path: '/defect-management/todo',
        component: lazy(() => import('@/views/concepts/defects/DefectList')),
        authority: [MG,CU,DM,DP,QA],
    },
    {
        key: 'defectCreate',
        path: '/defect-management/create',
        component: lazy(() => import('@/views/concepts/defects/DefectCreate')),
        authority: [],

    },
    {
        key: 'defectDetails',
        path: '/defect-management/details/:defectId',
        component: lazy(() => import('@/views/concepts/defects/DefectDetails/DefectDetails.jsx')),
        authority: [],
        meta: {
            pageContainerType: 'contained',
        },
    },
    {
        key: 'defectUpdate',
        path: '/defect-management/update/:defectId',
        component: lazy(() => import('@/views/concepts/defects/DefectEdit')),
        authority: [],
        meta: {
            pageContainerType: 'contained',
        },
    },
    ...othersRoute,
]