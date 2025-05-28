import useSWR from 'swr'
import { useProjectListStore } from '../store/ProjectListStore.js'
import { apiPrefix } from '@/configs/endpoint.config.js'
import { apiGetProjectList } from '@/services/axios/ProjectService.js'

export default function useProjectList() {
    const {
        tableData,
        filterData,
        setTableData,
        selectedProject,
        setSelectedProject,
        setSelectAllProject,
        setFilterData,
    } = useProjectListStore((state) => state)

    const adjustedTableData = {
        ...tableData,
        page: tableData.page,
        pageSize: tableData.pageSize,
        sortKey: tableData?.sort?.key || '',
        sortOrder: tableData?.sort?.order || '',
    }

    const { data, error, isLoading, mutate } = useSWR(
        [apiPrefix + '/projects/list', { ...adjustedTableData, ...filterData }],
        ([_, params]) => apiGetProjectList(params),
        {
            revalidateOnFocus: false,
        },
    )

    // 실제 content 배열만 추출
    const projectList = data?.content || []

    // 전체 항목 수 (→ 페이지 수가 아닌 실제 전체 데이터 수)
    const projectListTotal = data?.page?.totalElements || 0

    return {
        projectList,
        projectListTotal,
        error,
        isLoading,
        tableData,
        filterData,
        mutate,
        setTableData,
        selectedProject,
        setSelectedProject,
        setSelectAllProject,
        setFilterData,
    }
}