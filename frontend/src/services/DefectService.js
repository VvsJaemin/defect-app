import { apiPrefix } from '@/configs/endpoint.config.js'
import ApiService from '@/services/ApiService.js'
import isEmpty from 'lodash/isEmpty'

export async function apiGetDefectList(params, user) {
    const isAssignedPage = location.pathname === '/defect-management/assigned'
    const isInProgressPage =
        location.pathname === '/defect-management/in-progress'
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
        assigneeId: isEmpty(params.assigneeId) ? assigneeId : params.assigneeId,  // ✅ assignUserId → assigneeId 수정
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
