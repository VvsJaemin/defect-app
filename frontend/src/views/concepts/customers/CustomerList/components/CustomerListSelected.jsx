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
import useCustomerList from '../hooks/useCustomerList'
import { TbChecks } from 'react-icons/tb'
import { apiPrefix } from '@/configs/endpoint.config.js'

const CustomerListSelected = () => {
    const {
        selectedCustomer,
        // customerList,
        mutate,
        // customerListTotal,
        setSelectAllCustomer,
    } = useCustomerList()

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
            const userIdsToDelete = selectedCustomer.map((user) => user.userId)

            // 서버에 삭제 요청
            await fetch(apiPrefix + '/users/delete', {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(userIdsToDelete),
                credentials: 'include',
            })

            // 삭제 성공 후 사용자 목록 새로고침
            await mutate() // SWR 사용 시
            // 또는 getUsers() 같은 사용자 정의 fetch 함수 호출

            setSelectAllCustomer([])
            setDeleteConfirmationOpen(false)

            toast.push(
                <Notification title={'삭제 성공'} type="success">
                   선택하신 사용자가 성공적으로 삭제되었습니다.
                </Notification>,
            )

        } catch (error) {
            console.error('삭제 실패:', error)
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
            setSelectAllCustomer([])
        }, 500)
    }
    return (
        <>
            {selectedCustomer.length > 0 && (
                <StickyFooter
                    className=" flex items-center justify-between py-4 bg-white dark:bg-gray-800"
                    stickyClass="-mx-4 sm:-mx-8 border-t border-gray-200 dark:border-gray-700 px-8"
                    defaultClass="container mx-auto px-8 rounded-xl border border-gray-200 dark:border-gray-600 mt-4"
                >
                    <div className="container mx-auto">
                        <div className="flex items-center justify-between">
                            <span>
                                {selectedCustomer.length > 0 && (
                                    <span className="flex items-center gap-2">
                                        <span className="text-lg text-primary">
                                            <TbChecks />
                                        </span>
                                        <span className="font-semibold flex items-center gap-1">
                                            <span className="heading-text">
                                                {selectedCustomer.length}{' '}
                                                명의 사용자
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
                title="사용자 삭제"
                onClose={handleCancel}
                onRequestClose={handleCancel}
                onCancel={handleCancel}
                onConfirm={handleConfirmDelete}
                confirmText="삭제"
            >
                <p>
                  선택하신 사용자를 삭제하시겠습니까?
                </p>
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
                    {selectedCustomer.map((customer) => (
                        <Tooltip key={customer.userId} title={customer.userName}>
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

export default CustomerListSelected
