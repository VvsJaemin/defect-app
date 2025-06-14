import { apiPrefix } from '@/configs/endpoint.config.js'
import ApiService from '@/services/ApiService.js'

export async function apiGetDefectList(params) {

    const flattenedParams = {
        pageIndex: params.pageIndex,
        pageSize: params.pageSize,
        sortKey: params.sortKey,
        sortOrder: params.sortOrder,
        defectId: params.defectId,
        defectTitle: params.defectTitle,
    }

    return ApiService.fetchDataWithAxios({
        url: apiPrefix + '/defects/list',
        withCredentials: true,
        method: 'get',
        params: flattenedParams,
    })
}

export async function apiGetDefectRead({  ...params }) {
    return ApiService.fetchDataWithAxios({
        url: `/defectLogs/list/${params.defectId}`,
        method: 'get',
        withCredentials: true,
    })
}

export async function apiGetCustomerLog({ ...params }) {
    return ApiService.fetchDataWithAxios({
        url: `/customer/log`,
        method: 'get',
        params,
    })
}
