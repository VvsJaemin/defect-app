import { apiPrefix } from '@/configs/endpoint.config.js'
import ApiService from '@/services/ApiService.js'

export async function apiGetProjectList(params) {

    const flattenedParams = {
        pageIndex: params.pageIndex,
        pageSize: params.pageSize,
        sortKey: params.sortKey,
        sortOrder: params.sortOrder,
    }

    return ApiService.fetchDataWithAxios({
        url: apiPrefix + '/projects/list',
        withCredentials: true,
        method: 'get',
        params: flattenedParams,
    })
}

export async function apiGetProjectRead({  ...params }) {
    return ApiService.fetchDataWithAxios({
        url: `/projects/read`+ params.projectId,
        method: 'get',
        withCredentials: true,
        params,
    })
}

export async function apiGetCustomerLog({ ...params }) {
    return ApiService.fetchDataWithAxios({
        url: `/customer/log`,
        method: 'get',
        params,
    })
}
