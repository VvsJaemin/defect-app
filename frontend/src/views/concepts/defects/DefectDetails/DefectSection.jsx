import Button from '@/components/ui/Button'
import Notification from '@/components/ui/Notification'
import toast from '@/components/ui/toast'
import Select from '@/components/ui/Select'
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
import { DP, MG, QA } from '@/constants/roles.constant.js'

const DefectSection = ({ data = {} }) => {
    const navigate = useNavigate()
    const { user } = useAuth()

    // DefectRequestDto에 맞게 formData 구조 수정
    const [formData, setFormData] = useState({
        logCt: '',
        uploadedFile: null,
    })

    // 결함 이관을 위한 상태 추가
    const [showTransferSelect, setShowTransferSelect] = useState(false)
    const [userOptions, setUserOptions] = useState([])
    const [selectedUser, setSelectedUser] = useState(null)
    const [isLoadingUsers, setIsLoadingUsers] = useState(false)

    const handleBackToList = () => navigate('/defect-management')

    const handleLogCtChange = (value) => {
        setFormData((prev) => ({
            ...prev,
            logCt: value,
        }))
    }

    const handleFileChange = (file) => {
        setFormData((prev) => ({
            ...prev,
            uploadedFile: file,
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
            formDataToSend.append(
                'defectLogRequestDto',
                new Blob([JSON.stringify(requestData)], {
                    type: 'application/json',
                }),
            )

            // 파일을 FormData에 추가
            if (formData.uploadedFile) {
                formDataToSend.append('files', formData.uploadedFile)
                console.log(
                    '파일이 FormData에 추가됨:',
                    formData.uploadedFile.name,
                )
            } else {
                console.log('추가할 파일이 없습니다.')
            }

            // FormData 내용 확인
            for (let pair of formDataToSend.entries()) {
                console.log('FormData:', pair[0], pair[1])
            }

            await axios.post(`${apiPrefix}/defectLogs/save`, formDataToSend, {
                withCredentials: true,
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })

            toast.push(
                <Notification title={'조치 완료'} type="success">
                    조치가 완료되었습니다.
                </Notification>,
            )

            // 성공 후 폼 초기화
            setFormData({
                logCt: '',
                uploadedFile: null,
            })

            // 현재 페이지로 부드럽게 재이동 (깜박임 없이)
            navigate(`/defect-management/details/${data.content[0].defectId}`, {
                replace: true,
            })
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

    // 사용자 목록 가져오기
    const fetchUsers = async () => {
        setIsLoadingUsers(true)
        try {
            const response = await axios.get(
                `${apiPrefix}/projects/assignUserList`,
                {
                    withCredentials: true,
                },
            )

            const users = response.data
                .filter((user) => [MG, QA, DP].includes(user.userSeCd))
                .map((user) => ({
                    value: user.userId,
                    label: user.userName,
                }))

            setUserOptions(users)
        } catch (error) {
            console.error('사용자 목록을 가져오는 중 오류 발생:', error)
            toast.push(
                <Notification title={'데이터 로드 실패'} type="warning">
                    사용자 목록을 가져오는 중 오류가 발생했습니다.
                </Notification>,
            )
        } finally {
            setIsLoadingUsers(false)
        }
    }

    // 결함 이관 버튼 클릭 핸들러
    const handleDefectTransferClick = async () => {
        if (!showTransferSelect) {
            // SelectBox가 보이지 않을 때 - 사용자 목록을 가져오고 SelectBox 표시
            setShowTransferSelect(true)
            await fetchUsers()
        } else {
            // SelectBox가 이미 보일 때 - 선택된 사용자로 이관 처리
            if (!selectedUser) {
                toast.push(
                    <Notification title={'선택 필요'} type="warning">
                        이관할 사용자를 선택해주세요.
                    </Notification>,
                )
                return
            }
            await handleDefectTransfer()
        }
    }

    // 실제 결함 이관 처리
    const handleDefectTransfer = async () => {
        try {
            const formDataToSend = new FormData()

            // JSON 데이터를 FormData에 추가
            const requestData = {
                defectId: data.content[0].defectId,
                statusCd: 'DS7000', // 결함이관 상태 코드
                logTitle: '결함 담당자 이관',
                logCt: `${data.content[0].assignUserId}(${data.content[0].assignUserName})님이 담당자를 ${selectedUser.value}(${selectedUser.label})님으로 변경하였습니다.`,
                createdBy: user.userId,
                assignUserId: selectedUser.value,
            }

            // JSON 데이터를 Blob으로 변환하여 FormData에 추가
            formDataToSend.append(
                'defectLogRequestDto',
                new Blob([JSON.stringify(requestData)], {
                    type: 'application/json',
                }),
            )

            // 서버에 결함 수정 요청
            await axios.post(`${apiPrefix}/defectLogs/save`, formDataToSend, {
                withCredentials: true,
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })

            toast.push(
                <Notification title={'이관 완료'} type="success">
                    {selectedUser.label}님에게 결함이 이관되었습니다.
                </Notification>,
            )

            // 이관 후 상태 초기화
            setShowTransferSelect(false)
            setSelectedUser(null)
            setUserOptions([])

            navigate('/defect-management')
        } catch (error) {
            console.error('결함 이관 중 오류 발생:', error)
            toast.push(
                <Notification title={'이관 실패'} type="danger">
                    결함 이관에 실패했습니다.
                </Notification>,
            )
        }
    }

    // 이관 취소 핸들러
    const handleTransferCancel = () => {
        setShowTransferSelect(false)
        setSelectedUser(null)
        setUserOptions([])
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

            {/* 결함 이관 SelectBox 영역 */}
            {showTransferSelect && (
                <div className="pl-6 pr-6 py-4 bg-blue-50 border border-blue-200 rounded-lg mx-6">
                    <div className="space-y-4">
                        <h3 className="text-lg font-semibold text-gray-900">
                            결함 이관
                        </h3>
                        <div className="flex items-center space-x-4">
                            <div className="flex-1">
                                <Select
                                    placeholder="이관할 사용자를 선택하세요"
                                    options={userOptions}
                                    value={selectedUser}
                                    onChange={setSelectedUser}
                                    isLoading={isLoadingUsers}
                                    isSearchable
                                />
                            </div>
                            <div className="flex space-x-2">
                                <Button
                                    size="sm"
                                    variant="solid"
                                    onClick={handleDefectTransfer}
                                    disabled={!selectedUser || isLoadingUsers}
                                >
                                    이관
                                </Button>
                                <Button
                                    size="sm"
                                    variant="plain"
                                    onClick={handleTransferCancel}
                                >
                                    취소
                                </Button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

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
                        onClick={handleDefectTransferClick}
                    >
                        {showTransferSelect ? '사용자 선택 중...' : '결함 이관'}
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