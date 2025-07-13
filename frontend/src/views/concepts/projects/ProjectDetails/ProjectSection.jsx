import { useState } from 'react'
import Card from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Notification from '@/components/ui/Notification'
import Tooltip from '@/components/ui/Tooltip'
import toast from '@/components/ui/toast'
import ConfirmDialog from '@/components/shared/ConfirmDialog'
import dayjs from 'dayjs'
import { HiOutlineArrowLeft, HiOutlineTrash, HiPencil } from 'react-icons/hi'
import { useNavigate } from 'react-router'
import ApiService from '@/services/ApiService.js'

const ProjectInfoField = ({ title, value }) => {
    return (
        <div>
            <span className="font-semibold">{title}</span>
            <p className="heading-text font-bold">{value || '-'}</p>
        </div>
    )
}

const ProjectSection = ({ data = {} }) => {
    const navigate = useNavigate()

    const [dialogOpen, setDialogOpen] = useState(false)

    const handleBackToList = () => {
        navigate('/project-management')
    }

    const handleDialogClose = () => {
        setDialogOpen(false)
    }

    const handleDialogOpen = () => {
        setDialogOpen(true)
    }

    const handleDelete = async () => {
        try {
            let addProjectId = []
            addProjectId.push(data.projectId)

            // 서버에 삭제 요청
            await ApiService.delete('/projects/delete', {
                data: addProjectId, // DELETE 요청의 body는 data 속성에 넣어야 함
                headers: {
                    'Content-Type': 'application/json',
                },
                // credentials: 'include'와 동일
            })

            setDialogOpen(false)
            navigate('/project-management')
            toast.push(
                <Notification title={'프로젝트 삭제 성공'} type="success">
                    프로젝트가 성공적으로 삭제되었습니다
                </Notification>,
            )
        } catch (error) {
            toast.push(
                <Notification title={'삭제 실패'} type="danger">
                    {error.response?.data?.error ||
                        '프로젝트 삭제가 실패했습니다.'}
                </Notification>,
            )
        }
    }

    const handleEdit = () => {
        navigate(`/project-management/update/${data.projectId}`)
    }

    // 날짜 포맷 변환 함수
    const formatDate = (dateString) => {
        if (!dateString) return '-'
        return dayjs(dateString).format('YYYY-MM-DD HH:mm:ss')
    }

    // 프로젝트 상태 변환 함수
    const getStatusLabel = (statusCode) => {
        switch (statusCode) {
            case 'DEV':
                return '개발버전'
            case 'OPERATE':
                return '운영버전'
            case 'TEST':
                return '테스트버전'
            default:
                return statusCode || '-'
        }
    }

    // 할당된 사용자 목록 표시
    const renderAssignedUsers = () => {
        if (!data.assignedUsers || data.assignedUsers.length === 0) {
            return '-'
        }

        return (
            <ul className="list-disc pl-5">
                {data.assignedUsers.map((userId, index) => (
                    <li key={index}>
                        {data.assignedUsersMap?.[userId] || userId}
                    </li>
                ))}
            </ul>
        )
    }

    return (
        <Card className="w-full">
            <div className="flex justify-end">
                <Tooltip title="프로젝트 수정">
                    <button
                        className="close-button button-press-feedback"
                        type="button"
                        onClick={handleEdit}
                    >
                        <HiPencil />
                    </button>
                </Tooltip>
            </div>
            <div className="flex flex-col xl:justify-between h-full 2xl:min-w-[360px] mx-auto">
                <div className="flex xl:flex-col items-center gap-4 mt-3">
                    <h4 className="font-bold">{data.projectName}</h4>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-7 gap-x-4 mt-10">
                    <ProjectInfoField
                        title="프로젝트 ID"
                        value={data.projectId}
                    />
                    <ProjectInfoField title="URL" value={data.urlInfo} />
                    <ProjectInfoField
                        title="고객사"
                        value={data.customerName}
                    />
                    <ProjectInfoField
                        title="프로젝트 상태"
                        value={getStatusLabel(data.statusCode)}
                    />
                    <ProjectInfoField
                        title="프로젝트 설명"
                        value={data.etcInfo}
                    />
                    <div className="sm:col-span-2">
                        <span className="font-semibold">할당된 사용자</span>
                        <div className="heading-text font-bold">
                            {renderAssignedUsers()}
                        </div>
                    </div>
                    <ProjectInfoField
                        title="등록일시"
                        value={formatDate(data.createdAt)}
                    />
                    <ProjectInfoField
                        title="수정일시"
                        value={formatDate(data.updatedAt)}
                    />
                </div>
                <div className="flex flex-col gap-4 mt-6">
                    <div className="flex gap-2">
                        <Button
                            className="flex-1"
                            customColorClass={() =>
                                'text-error hover:border-error hover:ring-1 ring-error hover:text-error'
                            }
                            icon={<HiOutlineTrash />}
                            onClick={handleDialogOpen}
                        >
                            삭제하기
                        </Button>
                        <Button
                            className="flex-1"
                            icon={<HiOutlineArrowLeft />}
                            onClick={handleBackToList}
                        >
                            목록으로
                        </Button>
                    </div>
                </div>

                <ConfirmDialog
                    isOpen={dialogOpen}
                    type="danger"
                    title="프로젝트 삭제"
                    onClose={handleDialogClose}
                    onRequestClose={handleDialogClose}
                    onCancel={handleDialogClose}
                    onConfirm={handleDelete}
                    confirmText={'삭제'}
                >
                    <p>
                        이 프로젝트를 삭제하시겠습니까? 이 프로젝트와 관련된
                        모든 기록이 함께 삭제됩니다. 이 작업은 되돌릴 수
                        없습니다.
                    </p>
                </ConfirmDialog>
            </div>
        </Card>
    )
}

export default ProjectSection