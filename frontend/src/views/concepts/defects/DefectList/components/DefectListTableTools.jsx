import useDefectList from '../hooks/useDefectList.js'
import DefectListSearch from './DefectListSearch.jsx'
import cloneDeep from 'lodash/cloneDeep'

const DefectListTableTools = () => {
    const { tableData, setTableData, setFilterData, mutate } = useDefectList()

    const handleInputChange = (searchParams) => {

        setFilterData(searchParams);

        const newTableData = cloneDeep(tableData)
        newTableData.page = 1
        setTableData(newTableData)

        mutate()
    }

    // 초기화 핸들러
    const handleReset = () => {

        setFilterData({})

        const newTableData = cloneDeep(tableData)
        newTableData.page = 1  // pageIndex가 아닌 page 사용
        newTableData.query = '' // 검색 쿼리 초기화
        setTableData(newTableData)

        mutate(undefined, { revalidate: true })
    }

    return (
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-2">
            <DefectListSearch
                onInputChange={handleInputChange}
                onReset={handleReset}
            />
            {/*<CustomerTableFilter />*/}
        </div>
    )
}

export default DefectListTableTools
