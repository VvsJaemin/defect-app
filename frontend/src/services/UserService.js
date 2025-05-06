import ApiService from './ApiService'
import { apiPrefix } from '@/configs/endpoint.config.js'

export async function apiGetCustomersList(params) {

    return ApiService.fetchDataWithAxios({
        url: apiPrefix + '/users/list',
        withCredentials: true,
        method: 'get',
        params,
    })
}

export async function apiGetCustomer({ id, ...params }) {
    return ApiService.fetchDataWithAxios({
        url: `/customers/${id}`,
        method: 'get',
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
