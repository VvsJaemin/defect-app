import useCustomerList from '../hooks/useCustomerList'
import CustomerListSearch from './CustomerListSearch'
import cloneDeep from 'lodash/cloneDeep'

const CustomersListTableTools = () => {
    const { tableData, setTableData, filterData, setFilterData, mutate } = useCustomerList()


    const handleInputChange = (searchData) => {
        // 검색 조건 객체 생성
        const newFilterData = {}

        if(searchData.type && searchData.value) {
            newFilterData[searchData.type] = searchData.value
        }

        setFilterData(newFilterData)

        const newTableData = cloneDeep(tableData)
        newTableData.pageIndex = 1
        setTableData(newTableData)

        mutate()
    }

    // 초기화 핸들러
    const handleReset = () => {
        // 필터 데이터 완전히 초기화
        setFilterData({})

        // 테이블 데이터의 페이지 정보 초기화
        const newTableData = cloneDeep(tableData)
        newTableData.pageIndex = 1
        newTableData.query = '' // 검색 쿼리 초기화
        setTableData(newTableData)

        // API 강제 재호출 (캐시 무시)
        mutate(undefined, { revalidate: true })
    }




    return (
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-2">
            <CustomerListSearch onInputChange={handleInputChange}   onReset={handleReset}
            />
            {/*<CustomerTableFilter />*/}
        </div>
    )
}

export default CustomersListTableTools
