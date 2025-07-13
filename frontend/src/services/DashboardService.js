import ApiService from './ApiService'

export async function apiGetEcommerceDashboard() {
    return ApiService.fetchDataWithAxios({
        url: '/api/dashboard/ecommerce',
        method: 'get',
    })
}

export async function apiGetDefectDashboard() {
    try {
        const response = await ApiService.get('/defects/dashboard/list')
        return response.data
    } catch (error) {
        console.error('DefectService - 결함 대시보드 조회 오류:', error)
        console.error('DefectService - 오류 상세:', error.response?.data || error.message)
        throw error
    }
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