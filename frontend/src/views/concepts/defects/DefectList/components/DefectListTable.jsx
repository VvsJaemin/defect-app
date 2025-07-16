import { useMemo } from 'react'
import Tooltip from '@/components/ui/Tooltip'
import DataTable from '@/components/shared/DataTable'
import useDefectList from '../hooks/useDefectList.js'
import { useNavigate } from 'react-router'
import cloneDeep from 'lodash/cloneDeep'
import { TbPencil, TbEye } from 'react-icons/tb'
import { useAuth } from '@/auth/index.js'

const ActionColumn = ({ onEdit, onViewDetail, showEditButton }) => {
    return (
        <div className="flex items-center gap-3">
            {showEditButton && (
                <Tooltip title="결함 수정">
                    <div
                        className={`text-xl cursor-pointer select-none font-semibold`}
                        role="button"
                        onClick={onEdit}
                    >
                        <TbPencil />
                    </div>
                </Tooltip>
            )}
            <Tooltip title="결함내역 상세 및 추가">
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

const DefectListTable = ({path}) => {
    const navigate = useNavigate()

    const {
        defectList,
        defectListTotal,
        tableData,
        isLoading,
        setTableData,
        setSelectAllDefect,
        selectedDefect,
        setSelectedDefect,
    } = useDefectList(path)

    const { user } = useAuth();

    // 사용자 권한 확인 - MG 또는 QA인 경우에만 수정 및 선택 권한 부여
    const canEdit = user?.userSeCd === 'MG' || user?.userSeCd === 'QA';
    const canSelect = user?.userSeCd === 'MG' || user?.userSeCd === 'QA';

    const handleEdit = (defect) => {
        setSelectAllDefect(null) // 선택 해제
        navigate(`/defect-management/update/${defect.defectId}`)
    }

    const handleViewDetails = (defect) => {
        setSelectAllDefect(null) // 선택 해제
        navigate(`/defect-management/details/${defect.defectId}`)
    }

    const columns = useMemo(
        () => [
            {
                header: '결함아이디',
                accessorKey: 'defectId',
                size: 120,
            },
            {
                header: '프로젝트/사이트',
                accessorKey: 'projectName',
                size: 300,
            },
            {
                header: '결함요약',
                accessorKey: 'defectTitle',
                size: 300,
            },
            {
                header: '메뉴',
                accessorKey: 'defectMenuTitle',
                size: 250,
            },
            {
                header: '결함분류',
                accessorKey: 'defectDivCode',
                size: 150,
            },
            {
                header: '현재 담당',
                accessorKey: 'assigneeId',
                size: 150,
            },
            {
                header: '결함상태',
                accessorKey: 'statusCode',
                size: 150,
            },
            {
                header: '중요도',
                accessorKey: 'seriousCode',
                size: 150,
            },
            {
                header: '우선순위',
                accessorKey: 'orderCode',
                size: 150,
            },
            {
                header: '',
                id: 'action',
                size: 120,
                enableResizing: false, // 액션 컬럼은 리사이징 비활성화
                cell: (props) => {
                    return (
                        <ActionColumn
                            onEdit={() => handleEdit(props.row.original)}
                            onViewDetail={() => {
                                handleViewDetails(props.row.original);
                            }}
                            showEditButton={canEdit}
                        />
                    );
                }
            }

        ], // eslint-disable-next-line react-hooks/exhaustive-deps
        [canEdit],
    )

    const handleSetTableData = (data) => {
        setTableData(data)
        if (selectedDefect && selectedDefect.length > 0) {
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
            selectable={canSelect}
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
                selectedDefect?.defectId === row.defectId
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