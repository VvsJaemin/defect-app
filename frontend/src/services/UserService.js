import ApiService from './ApiService'
import { apiPrefix } from '@/configs/endpoint.config.js'

export async function apiGetCustomersList(params) {

    const flattenedParams = {
        pageIndex: params.pageIndex,
        pageSize: params.pageSize,
        sortKey: params.sortKey,
        sortOrder: params.sortOrder,
    }

    return ApiService.fetchDataWithAxios({
        url: apiPrefix + '/users/list',
        withCredentials: true,
        method: 'get',
        params: flattenedParams,
    })


}

export async function apiGetCustomer({  ...params }) {
    return ApiService.fetchDataWithAxios({
        url: `/users/read`,
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
