import { apiPrefix } from '@/configs/endpoint.config.js'
import ApiService from '@/services/ApiService.js'
import isEmpty from 'lodash/isEmpty'
import { tokenManager } from '@/utils/hooks/tokenManager.jsx'

export async function apiGetDefectList(params, user) {
    const isAssignedPage = location.pathname === '/defect-management/assigned'
    const isInProgressPage = location.pathname === '/defect-management/in-progress'
    const isCompletedPage = location.pathname === '/defect-management/completed'
    const isTodoPage = location.pathname === '/defect-management/todo'

    let chkType
    let assigneeId
    if (isAssignedPage) {
        chkType = 'assigned'
        assigneeId = user.userId
    }
    if (isInProgressPage) {
        chkType = 'in-progress'
    }
    if (isCompletedPage) {
        chkType = 'completed'
    }
    if (isTodoPage) {
        chkType = 'todo'
    }

    const flattenedParams = {
        pageIndex: params.pageIndex,
        pageSize: params.pageSize,
        sortKey: params.sortKey,
        sortOrder: params.sortOrder,
        defectId: params.defectId || '',
        defectTitle: params.defectTitle || '',
        projectId: params.projectId || '',
        statusCode: params.statusCode || '',
        type: chkType,
        assigneeId: isEmpty(params.assigneeId) ? assigneeId : params.assigneeId,
    }

    try {
        const response = await ApiService.get(`${apiPrefix}/defects/list`, {
            params: flattenedParams
        })
        return response.data
    } catch (error) {
        console.error('결함 목록 조회 오류:', error)
        throw error
    }
}

export async function apiGetDefectRead({ ...params }) {
    try {
        const response = await ApiService.get(`/defectLogs/list/${params.defectId}`)
        return response.data
    } catch (error) {
        console.error('결함 상세 조회 오류:', error)
        throw error
    }
}

export async function apiGetDefectDashboard() {
    try {

        // 쿠키 방식 인증 확인
        const isAuthenticated = tokenManager.isAuthenticated()
        const userInfo = tokenManager.getUserFromToken()

        const response = await ApiService.get('/defects/dashboard/list')
        return response.data
    } catch (error) {
        throw error
    }
}

export async function apiCreateDefect(defectData) {
    try {
        const response = await ApiService.post('/defects', defectData)
        return response.data
    } catch (error) {
        throw error
    }
}

export async function apiUpdateDefect(defectId, defectData) {
    try {
        const response = await ApiService.put(`/defects/${defectId}`, defectData)
        return response.data
    } catch (error) {
        throw error
    }
}

export async function apiDeleteDefect(defectId) {
    try {
        const response = await ApiService.delete(`/defects/${defectId}`)
        return response.data
    } catch (error) {
        throw error
    }
}

export async function apiGetDefectLogs(defectId) {
    try {
        const response = await ApiService.get(`/defectLogs/list/${defectId}`)
        return response.data
    } catch (error) {
        throw error
    }
}

export async function apiUploadDefectFile(defectId, fileData) {
    try {
        const formData = new FormData()
        formData.append('file', fileData)

        const response = await ApiService.post(`/defects/${defectId}/upload`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
        return response.data
    } catch (error) {
        throw error
    }
}