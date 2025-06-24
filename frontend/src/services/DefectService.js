import { apiPrefix } from '@/configs/endpoint.config.js'
import ApiService from '@/services/ApiService.js'

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
        defectId: params.defectId,
        defectTitle: params.defectTitle,
        type: chkType,
        assigneeId: assigneeId,
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
