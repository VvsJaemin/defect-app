import { useState } from 'react'
import Card from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Avatar from '@/components/ui/Avatar/Avatar'
import Notification from '@/components/ui/Notification'
import Tooltip from '@/components/ui/Tooltip'
import toast from '@/components/ui/toast'
import ConfirmDialog from '@/components/shared/ConfirmDialog'
import dayjs from 'dayjs'
import { HiOutlineArrowLeft, HiOutlineTrash, HiPencil } from 'react-icons/hi'
import { useNavigate } from 'react-router'
import { TbUser } from 'react-icons/tb'
import ApiService from '@/services/ApiService.js'

const CustomerInfoField = ({ title, value }) => {
    return (
        <div>
            <span className="font-semibold">{title}</span>
            <p className="heading-text font-bold">{value || '-'}</p>
        </div>
    )
}

const ProfileSection = ({ data = {} }) => {
    const navigate = useNavigate()

    const [dialogOpen, setDialogOpen] = useState(false)

    const handleBackToList = () => {
        navigate('/user-management')
    }

    const handleDialogClose = () => {
        setDialogOpen(false)
    }

    const handleDialogOpen = () => {
        setDialogOpen(true)
    }

    const handleDelete = async () => {
        try {
            let addUserId = []
            addUserId.push(data.userId)

            console.log(data.userSeCd)

            // 서버에 삭제 요청
            await ApiService.delete('/users/delete', {
                data: addUserId, // DELETE 요청의 body는 data 속성에 넣어야 함
            })

            setDialogOpen(false)
            navigate('/user-management')
            toast.push(
                <Notification title={'사용자 삭제'} type="success">
                    사용자가 성공적으로 삭제되었습니다
                </Notification>,
            )
        } catch (error) {
            let errorMessage = '사용자 삭제가 실패했습니다.'

            setDialogOpen(false)

            toast.push(
                <Notification title={'사용자 삭제 실패'} type="danger">
                    {errorMessage}
                </Notification>,
            )
        }
    }

    const handleEdit = () => {
        navigate(`/user-management/update/${data.userId}`)
    }

    // 날짜 포맷 변환 함수
    const formatDate = (dateString) => {
        if (!dateString) return '-'
        return dayjs(dateString).format('YYYY-MM-DD HH:mm:ss')
    }
    return (
        <Card className="w-full">
            <div className="flex justify-end">
                <Tooltip title="사용자 정보 수정">
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
                <div className="flex xl:flex-col items-center gap-4 mt-6">
                    <Avatar size={90} shape="circle" icon={<TbUser />} />
                    <h4 className="font-bold">{data.userName}</h4>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-1 gap-y-7 gap-x-4 mt-10">
                    <CustomerInfoField title="사용자 ID" value={data.userId} />
                    <CustomerInfoField title="권한" value={data.userSeNm} />
                    <CustomerInfoField
                        title="등록일시"
                        value={formatDate(data.firstRegDtm)}
                    />
                    <CustomerInfoField
                        title="최종 로그인 일시"
                        value={formatDate(data.lastLoginAt)}
                    />
                    <CustomerInfoField
                        title="수정일시"
                        value={formatDate(data.fnlUdtDtm)}
                    />
                </div>
                <div className="flex flex-col gap-4">
                    {/*<Button block variant="solid" onClick={handleSendMessage}>*/}
                    {/*    메시지 보내기*/}
                    {/*</Button>*/}
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
                    title="사용자 삭제"
                    onClose={handleDialogClose}
                    onRequestClose={handleDialogClose}
                    onCancel={handleDialogClose}
                    onConfirm={handleDelete}
                    confirmText={'삭제'}
                >
                    <p>
                        이 사용자를 삭제하시겠습니까? 이 사용자와 관련된 모든
                        기록이 함께 삭제됩니다. 이 작업은 되돌릴 수 없습니다.
                    </p>
                </ConfirmDialog>
            </div>
        </Card>
    )
}

export default ProfileSection