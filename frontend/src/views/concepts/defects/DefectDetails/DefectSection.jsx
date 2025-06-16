import Button from '@/components/ui/Button'
import Notification from '@/components/ui/Notification'
import toast from '@/components/ui/toast'
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
import { useState } from 'react'
import { useAuth } from '@/auth/index.js'

const DefectSection = ({ data = {} }) => {
    const navigate = useNavigate()
    const { user } = useAuth();


// DefectRequestDto에 맞게 formData 구조 수정
    const [formData, setFormData] = useState({
        logCt: '',
        uploadedFile: null
    })

    const handleBackToList = () => navigate('/defect-management')

    const handleLogCtChange = (value) => {
        setFormData(prev => ({
            ...prev,
            logCt: value
        }))
    }

    const handleFileChange = (file) => {
        setFormData(prev => ({
            ...prev,
            uploadedFile: file
        }))
    }


    const handleActionComplete = async () => {
        try {
            // FormData 객체 생성 (파일 업로드를 위해 필요)
            const formDataToSend = new FormData()

            // JSON 데이터를 FormData에 추가
            const requestData = {
                defectId: data.content[0].defectId,
                statusCd: 'DS3000', // 조치완료 상태 코드
                logTitle: '결함조치가 완료되었습니다.',
                logCt: formData.logCt,
                createdBy: user.userId,
            }

            console.log('logCt:', formData.logCt)
            console.log('uploadedFile:', formData.uploadedFile)

            // JSON 데이터를 Blob으로 변환하여 FormData에 추가
            formDataToSend.append('defectLogRequestDto', new Blob([JSON.stringify(requestData)], {
                type: 'application/json'
            }))

            // 파일을 FormData에 추가
            if (formData.uploadedFile) {
                formDataToSend.append('files', formData.uploadedFile)
                console.log('파일이 FormData에 추가됨:', formData.uploadedFile.name)
            } else {
                console.log('추가할 파일이 없습니다.')
            }

            // FormData 내용 확인
            for (let pair of formDataToSend.entries()) {
                console.log('FormData:', pair[0], pair[1])
            }

            await axios.post(
                `${apiPrefix}/defectLogs/save`,
                formDataToSend,
                {
                    withCredentials: true,
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                },
            )

            toast.push(
                <Notification title={'조치 완료'} type="success">
                    조치가 완료되었습니다.
                </Notification>,
            )

            // 성공 후 폼 초기화
            setFormData({
                logCt: '',
                uploadedFile: null
            })

            // 현재 페이지로 부드럽게 재이동 (깜박임 없이)
            navigate(`/defect-management/details/${data.content[0].defectId}`, { replace: true })

        } catch (error) {
            console.error('Error:', error)
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

    return (
        <div className="w-full min-h-screen space-y-4 bg-gray-50 pb-8">
            {/* 대제목 헤더 - 좌측 정렬 */}
            <div className="py-4 pl-6">
                <h1 className="text-3xl font-bold text-gray-900">
                    결함관리 ({data.content[0].defectId || 'N/A'})
                </h1>
            </div>

            {/* 처리 이력 영역 - 좌측 정렬 */}
            <div className="pl-6 pr-6 flex-1">
                <DefectTimeline
                    data={data}
                    logCt={formData.logCt}
                    uploadedFile={formData.uploadedFile}
                    onLogCtChange={handleLogCtChange}
                    onFileChange={handleFileChange}
                />

            </div>

            {/* 액션 버튼 영역 - 좌측 정렬 */}
            <div className="pl-6 pr-6 py-4 mt-auto">
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