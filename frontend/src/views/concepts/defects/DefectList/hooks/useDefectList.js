import useSWR from 'swr'
import { useDefectListStore } from '../store/DefectListStore.js'
import { apiPrefix } from '@/configs/endpoint.config.js'
import { apiGetDefectList } from '@/services/DefectService.js'

export default function useDefectList() {
    const {
        tableData,
        filterData,
        setTableData,
        selectedDefect,
        setSelectedDefect,
        setSelectAllDefect,
        setFilterData,
    } = useDefectListStore((state) => state)

    const adjustedTableData = {
        ...tableData,
        page: tableData.page,
        pageSize: tableData.pageSize,
        sortKey: tableData?.sort?.key || '',
        sortOrder: tableData?.sort?.order || '',

    }


    const { data, error, isLoading, mutate } = useSWR(
        [apiPrefix + '/defects/list', { ...adjustedTableData, ...filterData }],
        ([_, params]) => apiGetDefectList(params),
        {
            revalidateOnFocus: false,
        },
    )


    // 실제 content 배열만 추출
    const defectList = data?.content || []

    // 전체 항목 수 (→ 페이지 수가 아닌 실제 전체 데이터 수)
    const defectListTotal = data?.page?.totalElements || 0

    return {
        defectList,
        defectListTotal,
        error,
        isLoading,
        tableData,
        filterData,
        mutate,
        setTableData,
        selectedDefect,
        setSelectedDefect,
        setSelectAllDefect,
        setFilterData,
    }
}