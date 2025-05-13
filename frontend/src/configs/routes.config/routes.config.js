import { lazy } from 'react'
import authRoute from './authRoute'
import othersRoute from './othersRoute'
import { MG } from '@/constants/roles.constant.js'

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
        component: lazy(() => import('@/views/concepts/customers/CustomerList')),
        authority: [MG],
    },
    {
        key: 'userDetails',
        path: '/user-management/details/:userId',
        component: lazy(() => import('@/views/concepts/customers/CustomerDetails')),
        authority: [MG],
        meta: {
            pageContainerType: 'contained',
        },
    },
    {
        key: 'userInfoUpdate',
        path: '/user-management/update/:userId',
        component: lazy(() => import('@/views/concepts/customers/CustomerEdit')),
        authority: [MG],
        meta: {
            pageContainerType: 'contained',
        },
    },
    {
        key: 'userInfoCreate',
        path: '/user-management/create',
        component: lazy(() => import('@/views/concepts/customers/CustomerCreate')),
        authority: [MG],
        meta: {
            pageContainerType: 'contained',
        },
    },
    // 별도의 독립된 탭으로 사용자 정보 수정 추가
    {
        key: 'userInfoUpdate',
        path: '/user-management/update/:userId',
        component: lazy(() => import('@/views/concepts/customers/CustomerEdit')),

        authority: [MG],

    },
    {
        key: 'collapseMenu.item1',
        path: '/collapse-menu-item-view-1',
        component: lazy(() => import('@/views/demo/CollapseMenuItemView1')),
        authority: [],
    },
    {
        key: 'collapseMenu.item2',
        path: '/collapse-menu-item-view-2',
        component: lazy(() => import('@/views/demo/CollapseMenuItemView2')),
        authority: [],
    },
    {
        key: 'groupMenu.single',
        path: '/group-single-menu-item-view',
        component: lazy(() => import('@/views/demo/GroupSingleMenuItemView')),
        authority: [],
    },
    {
        key: 'groupMenu.collapse.item1',
        path: '/group-collapse-menu-item-view-1',
        component: lazy(
            () => import('@/views/demo/GroupCollapseMenuItemView1'),
        ),
        authority: [],
    },
    {
        key: 'groupMenu.collapse.item2',
        path: '/group-collapse-menu-item-view-2',
        component: lazy(
            () => import('@/views/demo/GroupCollapseMenuItemView2'),
        ),
        authority: [],
    },
    ...othersRoute,
]