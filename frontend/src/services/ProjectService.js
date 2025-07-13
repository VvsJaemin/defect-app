import { apiPrefix } from '@/configs/endpoint.config.js'
import ApiService from '@/services/ApiService.js'

export async function apiGetProjectList(params) {
    const flattenedParams = {
        pageIndex: params.pageIndex,
        pageSize: params.pageSize,
        sortKey: params.sortKey,
        sortOrder: params.sortOrder,
        customerName: params.customerName,
        projectName: params.projectName,
        urlInfo: params.urlInfo,
        projectState: params.projectState,
    }

    try {
        const response = await ApiService.get(`${apiPrefix}/projects/list`, {
            params: flattenedParams
        })
        return response.data
    } catch (error) {
        console.error('프로젝트 목록 조회 오류:', error)
        throw error
    }
}

export async function apiGetProjectRead({ ...params }) {
    try {
        const response = await ApiService.get('/projects/read', {
            params
        })
        return response.data
    } catch (error) {
        console.error('프로젝트 상세 조회 오류:', error)
        throw error
    }
}

export async function apiGetProjectLog({ ...params }) {
    try {
        const response = await ApiService.get('/customer/log', {
            params
        })
        return response.data
    } catch (error) {
        console.error('프로젝트 로그 조회 오류:', error)
        throw error
    }
}

export async function apiCreateProject(projectData) {
    try {
        const response = await ApiService.post('/projects', projectData)
        return response.data
    } catch (error) {
        console.error('프로젝트 생성 오류:', error)
        throw error
    }
}

export async function apiUpdateProject(projectId, projectData) {
    try {
        const response = await ApiService.put(`/projects/${projectId}`, projectData)
        return response.data
    } catch (error) {
        console.error('프로젝트 수정 오류:', error)
        throw error
    }
}

export async function apiDeleteProject(projectId) {
    try {
        const response = await ApiService.delete(`/projects/${projectId}`)
        return response.data
    } catch (error) {
        console.error('프로젝트 삭제 오류:', error)
        throw error
    }
}