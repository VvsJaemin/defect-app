import Button from '@/components/ui/Button'
import Notification from '@/components/ui/Notification'
import toast from '@/components/ui/toast'
import dayjs from 'dayjs'
import {
    HiOutlineArrowLeft,
    HiOutlineCheck,
    HiOutlineClipboardList,
    HiOutlinePause,
    HiOutlineShare,
} from 'react-icons/hi'
import { useNavigate } from 'react-router'
import { apiPrefix } from '@/configs/endpoint.config.js'
import axios from 'axios'
import DefectTimeline from '@/views/concepts/defects/DefectDetails/DefectTimeLine.jsx'


const DefectSection = ({ data = {} }) => {
    const navigate = useNavigate()

    const handleBackToList = () => navigate('/defect-management')

    const handleActionComplete = async () => {
        try {
            await axios.patch(
                `${apiPrefix}/defects/${data.content[0].defectId}/complete`,
                {},
                {
                    withCredentials: true,
                },
            )

            toast.push(
                <Notification title={'조치 완료'} type="success">
                    조치가 완료되었습니다.
                </Notification>,
            )
        } catch (error) {
            toast.push(
                <Notification title={'처리 실패'} type="danger">
                    {error.response?.data?.error ||
                        '조치 완료 처리에 실패했습니다.'}
                </Notification>,
            )
        }
    }

    const handleActionHold = async () => {
        try {
            await axios.patch(
                `${apiPrefix}/defects/${data.content[0].defectId}/hold`,
                {},
                {
                    withCredentials: true,
                },
            )

            toast.push(
                <Notification title={'조치 보류'} type="warning">
                    조치가 보류되었습니다 (결함아님).
                </Notification>,
            )
        } catch (error) {
            toast.push(
                <Notification title={'처리 실패'} type="danger">
                    {error.response?.data?.error ||
                        '조치 보류 처리에 실패했습니다.'}
                </Notification>,
            )
        }
    }

    const handleTodoProcess = async () => {
        try {
            await axios.patch(
                `${apiPrefix}/defects/${data.content[0].defectId}/todo`,
                {},
                {
                    withCredentials: true,
                },
            )

            toast.push(
                <Notification title={'TO DO 처리'} type="info">
                    TO DO로 처리되었습니다.
                </Notification>,
            )
        } catch (error) {
            toast.push(
                <Notification title={'처리 실패'} type="danger">
                    {error.response?.data?.error ||
                        'TO DO 처리에 실패했습니다.'}
                </Notification>,
            )
        }
    }

    const handleDefectTransfer = () => {
        navigate(`/defect-management/transfer/${data[0].defectId}`)
    }

    const formatDate = (dateString) => {
        if (!dateString) return '-'
        return dayjs(dateString).format('YYYY-MM-DD HH:mm:ss')
    }

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
        <div className="w-full max-h-screen space-y-4 bg-gray-50">
            {/* 대제목 헤더 - 좌측 정렬 */}
            <div className="py-4 pl-6">
                <h1 className="text-3xl font-bold text-gray-900">
                    결함관리 ({data.content[0].defectId || 'N/A'})
                </h1>
            </div>

            {/* 처리 이력 영역 - 좌측 정렬 */}
            <div className="pl-6 pr-6">
                <DefectTimeline data={data} />
            </div>

            {/* 액션 버튼 영역 - 좌측 정렬 */}
            <div className="pl-6 pr-6 py-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-6">
                    <Button
                        className="w-full text-base py-3"
                        customColorClass={() =>
                            'text-success hover:border-success hover:ring-1 ring-success hover:text-success'
                        }
                        icon={<HiOutlineCheck />}
                        onClick={handleActionComplete}
                    >
                        조치완료
                    </Button>
                    <Button
                        className="w-full text-base py-3"
                        customColorClass={() =>
                            'text-warning hover:border-warning hover:ring-1 ring-warning hover:text-warning'
                        }
                        icon={<HiOutlinePause />}
                        onClick={handleActionHold}
                    >
                        조치보류
                    </Button>
                    <Button
                        className="w-full text-base py-3"
                        customColorClass={() =>
                            'text-info hover:border-info hover:ring-1 ring-info hover:text-info'
                        }
                        icon={<HiOutlineClipboardList />}
                        onClick={handleTodoProcess}
                    >
                        TO DO 처리
                    </Button>
                    <Button
                        className="w-full text-base py-3"
                        customColorClass={() =>
                            'text-primary hover:border-primary hover:ring-1 ring-primary hover:text-primary'
                        }
                        icon={<HiOutlineShare />}
                        onClick={handleDefectTransfer}
                    >
                        결함 이관
                    </Button>
                    <Button
                        className="w-full text-base py-3"
                        icon={<HiOutlineArrowLeft />}
                        onClick={handleBackToList}
                    >
                        목록으로
                    </Button>
                </div>
            </div>
        </div>
    )
}

export default DefectSection