import { useMemo } from 'react'
import Tooltip from '@/components/ui/Tooltip'
import DataTable from '@/components/shared/DataTable'
import useDefectList from '../hooks/useDefectList.js'
import { useNavigate } from 'react-router'
import cloneDeep from 'lodash/cloneDeep'
import { TbPencil, TbEye } from 'react-icons/tb'

const ActionColumn = ({ onEdit, onViewDetail }) => {
    return (
        <div className="flex items-center gap-3">
            <Tooltip title="결함 수정">
                <div
                    className={`text-xl cursor-pointer select-none font-semibold`}
                    role="button"
                    onClick={onEdit}
                >
                    <TbPencil />
                </div>
            </Tooltip>
            <Tooltip title="결함내역 상세">
                <div
                    className={`text-xl cursor-pointer select-none font-semibold`}
                    role="button"
                    onClick={onViewDetail}
                >
                    <TbEye />
                </div>
            </Tooltip>
        </div>
    )
}

const DefectListTable = () => {
    const navigate = useNavigate()

    const {
        defectList,
        defectListTotal,
        tableData,
        isLoading,
        setTableData,
        selectAllDefect,
        setSelectAllDefect,
        selectedDefect,
        setSelectedDefect
    } = useDefectList()


    const handleEdit = (defect) => {
        navigate(`/defect-management/update/${defect.defectId}`)
    }

    const handleViewDetails = (defect) => {
        navigate(`/defect-management/details/${defect.defectId}`)
    }

    const columns = useMemo(
        () => [
            {
                header: '결함 번호',
                accessorKey: 'defectId',
            },
            {
                header: '결함명',
                accessorKey: 'defectName',
            },
            {
                header: 'URL',
                accessorKey: 'urlInfo',
            },
            {
                header: '고객사',
                accessorKey: 'customerName',
            },
            {
                header: '상태',
                accessorKey: 'statusCode',
            },
            {
                header: '할당인원수',
                accessorKey: 'assignedUserCnt',
            },
            {
                header: '등록일시',
                accessorKey: 'createdAt',
            },
            {
                header: '',
                id: 'action',
                cell: (props) => {
                    return (
                        <ActionColumn
                            onEdit={() => handleEdit(props.row.original)}
                            onViewDetail={() => {
                                handleViewDetails(props.row.original);
                            }}
                        />
                    );
                }
            }

        ], // eslint-disable-next-line react-hooks/exhaustive-deps
        [],
    )

    const handleSetTableData = (data) => {
        setTableData(data)
        if (selectedDefect.length > 0) {
            setSelectAllDefect([])
        }
    }

    const handlePaginationChange = (page) => {
        const newTableData = cloneDeep(tableData)
        newTableData.pageIndex = page
        handleSetTableData(newTableData)
    }

    const handleSelectChange = (value) => {
        const newTableData = cloneDeep(tableData)
        newTableData.pageSize = Number(value)
        newTableData.pageIndex = 1
        newTableData.defectState = tableData.defectState
        handleSetTableData(newTableData)
    }

    const handleSort = (sort) => {
        const newTableData = cloneDeep(tableData)
        newTableData.sort = sort
        handleSetTableData(newTableData)
    }

    const handleRowSelect = (checked, row) => {
        setSelectedDefect(checked, row)
    }

    const handleAllRowSelect = (checked, rows) => {
        if (checked) {
            const originalRows = rows.map((row) => row.original)
            setSelectAllDefect(originalRows)
        } else {
            setSelectAllDefect([])
        }
    }

    return (
        <DataTable
            selectable
            columns={columns}
            data={defectList}
            noData={!isLoading && defectList.length === 0}
            skeletonAvatarColumns={[0]}
            skeletonAvatarProps={{ width: 28, height: 28 }}
            loading={isLoading}
            pagingData={{
                total: defectListTotal,
                pageIndex: tableData.page,
                pageSize: tableData.pageSize,
                defectState: tableData.defectState,
            }}
            checkboxChecked={(row) =>
                selectedDefect.some((selected) => selected.defectId === row.defectId)
            }
            onPaginationChange={handlePaginationChange}
            onSelectChange={handleSelectChange}
            onSort={handleSort}
            onCheckBoxChange={handleRowSelect}
            onIndeterminateCheckBoxChange={handleAllRowSelect}
        />
    )
}

export default DefectListTable