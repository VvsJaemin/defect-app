import ApiService from './ApiService'
import { apiPrefix } from '@/configs/endpoint.config.js'

export async function apiGetEcommerceDashboard() {
    return ApiService.fetchDataWithAxios({
        url: '/api/dashboard/ecommerce',
        method: 'get',
    })
}

export async function apiGetDefectDashboard() {
    return ApiService.fetchDataWithAxios({
        url: apiPrefix + '/defects/dashboard/list',
        withCredentials: true,
        method: 'get',
    })
}

export async function apiGetProjectDashboard() {
    return ApiService.fetchDataWithAxios({
        url: '/api/dashboard/project',
        method: 'get',
    })
}

export async function apiGetAnalyticDashboard() {
    return ApiService.fetchDataWithAxios({
        url: '/api/dashboard/analytic',
        method: 'get',
    })
}

export async function apiGetMarketingDashboard() {
    return ApiService.fetchDataWithAxios({
        url: '/api/dashboard/marketing',
        method: 'get',
    })
}
