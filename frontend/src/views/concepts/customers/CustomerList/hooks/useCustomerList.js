import { apiGetCustomersList } from '@/services/UserService'
import useSWR from 'swr'
import { useCustomerListStore } from '../store/customerListStore'
import { apiPrefix } from '@/configs/endpoint.config.js'

export default function useCustomerList() {
    const {
        tableData,
        filterData,
        setTableData,
        selectedCustomer,
        setSelectedCustomer,
        setSelectAllCustomer,
        setFilterData,
    } = useCustomerListStore((state) => state)

    const adjustedTableData = {
        ...tableData,
        page: tableData.page ,  // 서버는 0-based index 필요
        pageSize: tableData.pageSize,
    }

    const { data, error, isLoading, mutate } = useSWR(
        [apiPrefix + '/users/list', { ...adjustedTableData, ...filterData }],
        ([_, params]) => apiGetCustomersList(params),
        {
            revalidateOnFocus: false,
        },
    )

    // 실제 content 배열만 추출
    const customerList = data?.content || []

    // 전체 항목 수 (→ 페이지 수가 아닌 실제 전체 데이터 수)
    const customerListTotal = data?.page?.totalElements || 0

    return {
        customerList,
        customerListTotal,
        error,
        isLoading,
        tableData,
        filterData,
        mutate,
        setTableData,
        selectedCustomer,
        setSelectedCustomer,
        setSelectAllCustomer,
        setFilterData,
    }
}
