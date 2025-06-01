import { useMemo } from 'react'
import Avatar from '@/components/ui/Avatar'
import Tag from '@/components/ui/Tag'
import Tooltip from '@/components/ui/Tooltip'
import DataTable from '@/components/shared/DataTable'
import useProjectList from '../hooks/useProjectList.js'
import { Link, useNavigate } from 'react-router'
import cloneDeep from 'lodash/cloneDeep'
import { TbPencil, TbEye } from 'react-icons/tb'

// const statusColor = {
//     active: 'bg-emerald-200 dark:bg-emerald-200 text-gray-900 dark:text-gray-900',
//     blocked: 'bg-red-200 dark:bg-red-200 text-gray-900 dark:text-gray-900',
// }
//
// const NameColumn = ({ row }) => {
//     return (
//         <div className="flex items-center">
//             <Avatar size={40} shape="circle" src={row.img} />
//             <Link
//                 className={`hover:text-primary ml-2 rtl:mr-2 font-semibold text-gray-900 dark:text-gray-100`}
//                 to={`/concepts/customers/customer-details/${row.id}`}
//             >
//                 {row.name}
//             </Link>
//         </div>
//     )
// }

const ActionColumn = ({ onEdit, onViewDetail }) => {
    return (
        <div className="flex items-center gap-3">
            <Tooltip title="프로젝트 수정">
                <div
                    className={`text-xl cursor-pointer select-none font-semibold`}
                    role="button"
                    onClick={onEdit}
                >
                    <TbPencil />
                </div>
            </Tooltip>
            <Tooltip title="프로젝트 상세">
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

const ProjectListTable = () => {
    const navigate = useNavigate()

    const {
        projectList,
        projectListTotal,
        tableData,
        isLoading,
        setTableData,
        setSelectAllProject,
        setSelectedProject,
        selectedProject,
    } = useProjectList()

    console.log(projectList)

    const handleEdit = (project) => {
        navigate(`/project-management/update/${project.projectId}`)

    }

    const handleViewDetails = (project) => {
        navigate(`/project-management/details/${project.projectId}`)
    }

    const columns = useMemo(
        () => [
            {
                header: '프로젝트 번호',
                accessorKey: 'projectId',
            },
            {
                header: '프로젝트명',
                accessorKey: 'projectName',
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
        if (selectedProject.length > 0) {
            setSelectAllProject([])
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
        newTableData.projectState = tableData.projectState
        handleSetTableData(newTableData)
    }

    const handleSort = (sort) => {
        const newTableData = cloneDeep(tableData)
        newTableData.sort = sort
        handleSetTableData(newTableData)
    }

    const handleRowSelect = (checked, row) => {
        setSelectedProject(checked, row)
    }

    const handleAllRowSelect = (checked, rows) => {
        if (checked) {
            const originalRows = rows.map((row) => row.original)
            setSelectAllProject(originalRows)
        } else {
            setSelectAllProject([])
        }
    }

    return (
        <DataTable
            selectable
            columns={columns}
            data={projectList}
            noData={!isLoading && projectList.length === 0}
            skeletonAvatarColumns={[0]}
            skeletonAvatarProps={{ width: 28, height: 28 }}
            loading={isLoading}
            pagingData={{
                total: projectListTotal,
                pageIndex: tableData.page,
                pageSize: tableData.pageSize,
                projectState: tableData.projectState,
            }}
            checkboxChecked={(row) =>
                selectedProject.some((selected) => selected.projectId === row.projectId )
            }
            onPaginationChange={handlePaginationChange}
            onSelectChange={handleSelectChange}
            onSort={handleSort}
            onCheckBoxChange={handleRowSelect}
            onIndeterminateCheckBoxChange={handleAllRowSelect}
        />
    )
}

export default ProjectListTable
