import { useMemo } from 'react'
import Avatar from '@/components/ui/Avatar'
import Tag from '@/components/ui/Tag'
import Tooltip from '@/components/ui/Tooltip'
import DataTable from '@/components/shared/DataTable'
import useUserList from '../hooks/useUserList.js'
import { Link, useNavigate } from 'react-router'
import cloneDeep from 'lodash/cloneDeep'
import { TbPencil, TbEye } from 'react-icons/tb'

const statusColor = {
    active: 'bg-emerald-200 dark:bg-emerald-200 text-gray-900 dark:text-gray-900',
    blocked: 'bg-red-200 dark:bg-red-200 text-gray-900 dark:text-gray-900',
}

const NameColumn = ({ row }) => {
    return (
        <div className="flex items-center">
            <Avatar size={40} shape="circle" src={row.img} />
            <Link
                className={`hover:text-primary ml-2 rtl:mr-2 font-semibold text-gray-900 dark:text-gray-100`}
                to={`/concepts/customers/customer-details/${row.id}`}
            >
                {row.name}
            </Link>
        </div>
    )
}

const ActionColumn = ({ onEdit, onViewDetail }) => {
    return (
        <div className="flex items-center gap-3">
            <Tooltip title="사용자 수정">
                <div
                    className={`text-xl cursor-pointer select-none font-semibold`}
                    role="button"
                    onClick={onEdit}
                >
                    <TbPencil />
                </div>
            </Tooltip>
            <Tooltip title="사용자 상세">
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

const UserListTable = () => {
    const navigate = useNavigate()

    const {
        customerList,
        customerListTotal,
        tableData,
        isLoading,
        setTableData,
        setSelectAllCustomer,
        setSelectedCustomer,
        selectedCustomer,
    } = useUserList()

    // const handleEdit = (customer) => {
    //     navigate(`/user-management/update/${customer.userId}`)
    // }
    //
    // const handleViewDetails = (customer) => {
    //     navigate(`/user-management/details/${customer.userId}`)
    // }

    const handleEdit = (customer) => {
        // 한글 userId를 URL 인코딩해서 navigate
        alert('수정 버튼 클릭 - 원본 userId:', customer.userId)
        const encodedUserId = encodeURIComponent(customer.userId)
        alert('인코딩된 userId:', encodedUserId)
        navigate(`/user-management/update/${encodedUserId}`)
    }

    const handleViewDetails = (customer) => {
        // 한글 userId를 URL 인코딩해서 navigate
        alert('상세보기 버튼 클릭 - 원본 userId:', customer.userId)
        const encodedUserId = encodeURIComponent(customer.userId)
        alert('인코딩된 userId:', encodedUserId)
        navigate(`/user-management/details/${encodedUserId}`)
    }


    const columns = useMemo(
        () => [
            {
                header: '계정',
                accessorKey: 'userId',
                size: 120,
            },
            {
                header: '사용자명',
                accessorKey: 'userName',
                size: 150,
            },
            {
                header: '권한',
                accessorKey: 'userSeCd',
                size: 100,
            },
            {
                header: '등록일시',
                accessorKey: 'createdAt',
                size: 180,
            },
            {
                header: '최종 로그인 일시',
                accessorKey: 'lastLoginAt',
                size: 180,
            },
            {
                header: '비밀번호 오류 횟수',
                accessorKey: 'pwdFailCnt',
                size: 120,
                meta: {
                    align: 'center',
                },
                cell: (props) => (
                    <div className="text-center">
                        {props.getValue()}
                    </div>
                ),
            },
            {
                header: '',
                id: 'action',
                size: 100,
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
        if (selectedCustomer.length > 0) {
            setSelectAllCustomer([])
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
        newTableData.userSeCd = tableData.userSeCd
        handleSetTableData(newTableData)
    }

    const handleSort = (sort) => {
        const newTableData = cloneDeep(tableData)
        newTableData.sort = sort
        handleSetTableData(newTableData)
    }

    const handleRowSelect = (checked, row) => {
        setSelectedCustomer(checked, row)
    }

    const handleAllRowSelect = (checked, rows) => {
        if (checked) {
            const originalRows = rows.map((row) => row.original)
            setSelectAllCustomer(originalRows)
        } else {
            setSelectAllCustomer([])
        }
    }

    return (
        <DataTable
            selectable
            columns={columns}
            data={customerList}
            noData={!isLoading && customerList.length === 0}
            skeletonAvatarColumns={[0]}
            skeletonAvatarProps={{ width: 28, height: 28 }}
            loading={isLoading}
            pagingData={{
                total: customerListTotal,
                pageIndex: tableData.page,
                pageSize: tableData.pageSize,
                userSeCd : tableData.userSeCd,
            }}
            checkboxChecked={(row) =>
                selectedCustomer.some((selected) => selected.userId === row.userId )
            }
            onPaginationChange={handlePaginationChange}
            onSelectChange={handleSelectChange}
            onSort={handleSort}
            onCheckBoxChange={handleRowSelect}
            onIndeterminateCheckBoxChange={handleAllRowSelect}
        />
    )
}

export default UserListTable
