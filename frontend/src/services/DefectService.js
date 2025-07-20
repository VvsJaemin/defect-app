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

    const response = await ApiService.get(`${apiPrefix}/defects/list`, {
        params: flattenedParams
    })
    return response.data
}

export async function apiGetDefectRead({ ...params }) {
    const response = await ApiService.get(`/defectLogs/list/${params.defectId}`)
    return response.data
}

export async function apiGetDefectDashboard() {
    // 인증 확인
    if (!tokenManager.isAuthenticated()) {
        throw new Error('인증이 필요합니다.')
    }

    const response = await ApiService.get('/defects/dashboard/list')
    return response.data
}

export async function apiCreateDefect(defectData) {
    const response = await ApiService.post('/defects', defectData)
    return response.data
}

export async function apiUpdateDefect(defectId, defectData) {
    const response = await ApiService.put(`/defects/${defectId}`, defectData)
    return response.data
}

export async function apiDeleteDefect(defectId) {
    const response = await ApiService.delete(`/defects/${defectId}`)
    return response.data
}

export async function apiGetDefectLogs(defectId) {
    const response = await ApiService.get(`/defectLogs/list/${defectId}`)
    return response.data
}

export async function apiUploadDefectFile(defectId, fileData) {
    const formData = new FormData()
    formData.append('file', fileData)

    const response = await ApiService.post(`/defects/${defectId}/upload`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
    return response.data
}