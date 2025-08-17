import { useState } from 'react'
import StickyFooter from '@/components/shared/StickyFooter'
import Button from '@/components/ui/Button'
import Dialog from '@/components/ui/Dialog'
import Avatar from '@/components/ui/Avatar'
import Tooltip from '@/components/ui/Tooltip'
import Notification from '@/components/ui/Notification'
import toast from '@/components/ui/toast'
import RichTextEditor from '@/components/shared/RichTextEditor'
import ConfirmDialog from '@/components/shared/ConfirmDialog'
import useProjectList from '../hooks/useProjectList.js'
import { TbChecks } from 'react-icons/tb'
import ApiService from '@/services/ApiService'

const ProjectListSelected = () => {
    const {
        selectedProject,
        // customerList,
        mutate,
        // customerListTotal,
        setSelectAllProject,
    } = useProjectList()

    const [deleteConfirmationOpen, setDeleteConfirmationOpen] = useState(false)
    const [sendMessageDialogOpen, setSendMessageDialogOpen] = useState(false)
    const [sendMessageLoading, setSendMessageLoading] = useState(false)

    const handleDelete = () => {
        setDeleteConfirmationOpen(true)
    }

    const handleCancel = () => {
        setDeleteConfirmationOpen(false)
    }

    const handleConfirmDelete = async () => {
        try {
            const projectIdToDelete = selectedProject.map((project) => project.projectId)

            // ApiService를 사용하여 삭제 요청
            await ApiService.delete('/projects/delete', {
                data: projectIdToDelete,
            })

            // 삭제 성공 후 사용자 목록 새로고침
            await mutate() // SWR 사용 시
            // 또는 getUsers() 같은 사용자 정의 fetch 함수 호출

            setSelectAllProject([])
            setDeleteConfirmationOpen(false)

            toast.push(
                <Notification title={'삭제 성공'} type="success">
                    프로젝트가 성공적으로 삭제되었습니다.
                </Notification>,
            )

        } catch (error) {
            console.error('삭제 실패:', error)
            toast.push(
                <Notification title={'삭제 실패'} type="danger">
                    프로젝트 삭제 중 오류가 발생했습니다.
                </Notification>,
            )
        }
    }


    const handleSend = () => {
        setSendMessageLoading(true)
        setTimeout(() => {
            toast.push(
                <Notification type="success">Message sent!</Notification>,
                { placement: 'top-center' },
            )
            setSendMessageLoading(false)
            setSendMessageDialogOpen(false)
            setSelectAllProject([])
        }, 500)
    }
    return (
        <>
            {selectedProject.length > 0 && (
                <StickyFooter
                    className=" flex items-center justify-between py-4 bg-white dark:bg-gray-800"
                    stickyClass="-mx-4 sm:-mx-8 border-t border-gray-200 dark:border-gray-700 px-8"
                    defaultClass="container mx-auto px-8 rounded-xl border border-gray-200 dark:border-gray-600 mt-4"
                >
                    <div className="container mx-auto">
                        <div className="flex items-center justify-between">
                            <span>
                                {selectedProject.length > 0 && (
                                    <span className="flex items-center gap-2">
                                        <span className="text-lg text-primary">
                                            <TbChecks />
                                        </span>
                                        <span className="font-semibold flex items-center gap-1">
                                            <span className="heading-text">
                                                {selectedProject.length}{' '}
                                                개의 프로젝트
                                            </span>
                                            <span>선택</span>
                                        </span>
                                    </span>
                                )}
                            </span>

                            <div className="flex items-center">
                                <Button
                                    size="sm"
                                    className="ltr:mr-3 rtl:ml-3"
                                    type="button"
                                    customColorClass={() =>
                                        'border-error ring-1 ring-error text-error hover:border-error hover:ring-error hover:text-error'
                                    }
                                    onClick={handleDelete}
                                >
                                    삭제
                                </Button>
                                {/*<Button*/}
                                {/*    size="sm"*/}
                                {/*    variant="solid"*/}
                                {/*    onClick={() =>*/}
                                {/*        setSendMessageDialogOpen(true)*/}
                                {/*    }*/}
                                {/*>*/}
                                {/*    Message*/}
                                {/*</Button>*/}
                            </div>
                        </div>
                    </div>
                </StickyFooter>
            )}

            <ConfirmDialog
                isOpen={deleteConfirmationOpen}
                type="danger"
                title="프로젝트 삭제"
                onClose={handleCancel}
                onRequestClose={handleCancel}
                onCancel={handleCancel}
                onConfirm={handleConfirmDelete}
                confirmText="삭제"
            >
                <div className="space-y-3">
                    <p>
                        선택하신 프로젝트를 삭제하시겠습니까?
                    </p>
                    <p className="text-sm text-gray-600 dark:text-gray-400 bg-yellow-50 dark:bg-yellow-900/20 p-3 rounded-md border border-yellow-200 dark:border-yellow-800">
                        <span className="font-medium text-yellow-800 dark:text-yellow-200">⚠️ 주의:</span> 프로젝트를 삭제하면 해당 프로젝트의 모든 결함 정보와 결함 이력도 함께 삭제됩니다.
                    </p>
                </div>
            </ConfirmDialog>
            <Dialog
                isOpen={sendMessageDialogOpen}
                onRequestClose={() => setSendMessageDialogOpen(false)}
                onClose={() => setSendMessageDialogOpen(false)}
            >
                <h5 className="mb-2">Send Message</h5>
                <p>Send message to the following customers</p>
                <Avatar.Group
                    chained
                    omittedAvatarTooltip
                    className="mt-4"
                    maxCount={4}
                    omittedAvatarProps={{ size: 30 }}
                >
                    {selectedProject.map((customer) => (
                        <Tooltip key={customer.projectId} title={customer.projectName}>
                            <Avatar size={30} src={customer.img} alt="" />
                        </Tooltip>
                    ))}
                </Avatar.Group>
                <div className="my-4">
                    <RichTextEditor content={''} />
                </div>
                <div className="ltr:justify-end flex items-center gap-2">
                    <Button
                        size="sm"
                        onClick={() => setSendMessageDialogOpen(false)}
                    >
                        Cancel
                    </Button>
                    <Button
                        size="sm"
                        variant="solid"
                        loading={sendMessageLoading}
                        onClick={handleSend}
                    >
                        Send
                    </Button>
                </div>
            </Dialog>
        </>
    )
}

export default ProjectListSelected