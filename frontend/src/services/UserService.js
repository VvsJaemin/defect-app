import ApiService from './ApiService'
import { apiPrefix } from '@/configs/endpoint.config.js'

export async function apiGetUsersList(params) {
    const flattenedParams = {
        pageIndex: params.pageIndex,
        pageSize: params.pageSize,
        sortKey: params.sortKey,
        sortOrder: params.sortOrder,
        userId: params.userId,
        userName: params.userName,
        userSeCd: params.userSeCd,
    }

    try {
        const response = await ApiService.get(`${apiPrefix}/users/list`, {
            params: flattenedParams
        })
        return response.data
    } catch (error) {
        console.error('사용자 목록 조회 오류:', error)
        throw error
    }
}

export async function apiGetUser({ ...params }) {
    try {
        const response = await ApiService.get('/users/read', {
            params
        })
        return response.data
    } catch (error) {
        console.error('사용자 상세 조회 오류:', error)
        throw error
    }
}

export async function apiGetUserLog({ ...params }) {
    try {
        const response = await ApiService.get('/customer/log', {
            params
        })
        return response.data
    } catch (error) {
        console.error('사용자 로그 조회 오류:', error)
        throw error
    }
}

export async function apiCreateUser(userData) {
    try {
        const response = await ApiService.post('/users', userData)
        return response.data
    } catch (error) {
        console.error('사용자 생성 오류:', error)
        throw error
    }
}

export async function apiUpdateUser(userId, userData) {
    try {
        const response = await ApiService.put(`/users/${userId}`, userData)
        return response.data
    } catch (error) {
        console.error('사용자 수정 오류:', error)
        throw error
    }
}

export async function apiDeleteUser(userId) {
    try {
        const response = await ApiService.delete(`/users/${userId}`)
        return response.data
    } catch (error) {
        console.error('사용자 삭제 오류:', error)
        throw error
    }
}