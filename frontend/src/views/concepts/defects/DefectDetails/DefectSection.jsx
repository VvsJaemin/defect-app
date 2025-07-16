import Button from '@/components/ui/Button'
import Notification from '@/components/ui/Notification'
import toast from '@/components/ui/toast'
import Select from '@/components/ui/Select'
import {
    HiOutlineArrowLeft,
    HiOutlineCheck,
    HiOutlineCheckCircle,
    HiOutlineClipboardList,
    HiOutlinePause,
    HiOutlineShare,
    HiOutlineX,
} from 'react-icons/hi'
import { useNavigate } from 'react-router'
import DefectTimeline from '@/views/concepts/defects/DefectDetails/DefectTimeLine.jsx'
import { useState } from 'react'
import { useAuth } from '@/auth/index.js'
import { DP, MG, QA } from '@/constants/roles.constant.js'
import ConfirmDialog from '@/components/shared/ConfirmDialog.jsx'
import ApiService from '@/services/ApiService.js'

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

    // 다이얼로그 상태 추가
    const [dialogStates, setDialogStates] = useState({
        actionComplete: false,
        actionHold: false,
        todoProcess: false,
        todoConfirm: false,
        defectClose: false,
        defectRelease: false,
        defectReoccurrence: false,
        defectReject: false,
        defectTransfer: false,
    })

    // 다이얼로그 열기 함수들
    const openDialog = (dialogType) => {
        setDialogStates((prev) => ({ ...prev, [dialogType]: true }))
    }

    // 다이얼로그 닫기 함수
    const closeDialog = (dialogType) => {
        setDialogStates((prev) => ({ ...prev, [dialogType]: false }))
    }

    const handleBackToList = () =>navigate('/defect-management/in-progress')

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
            } else {
                console.log('추가할 파일이 없습니다.')
            }

            await ApiService.post('/defectLogs/save', formDataToSend, {
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

            // 다이얼로그 닫기
            closeDialog('actionComplete')

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
            // FormData 객체 생성 (파일 업로드를 위해 필요)
            const formDataToSend = new FormData()

            // JSON 데이터를 FormData에 추가
            const requestData = {
                defectId: data.content[0].defectId,
                statusCd: 'DS4000', // 조치보류 상태 코드
                logTitle: '조치보류 처리 되었습니다.',
                logCt: formData.logCt,
                createdBy: user.userId,
            }

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
            }

            // FormData 내용 확인
            for (let pair of formDataToSend.entries()) {
                console.log('FormData:', pair[0], pair[1])
            }

            await ApiService.post('/defectLogs/save', formDataToSend, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })

            toast.push(
                <Notification title={'조치 보류'} type="warning">
                    조치가 보류되었습니다 (결함아님).
                </Notification>,
            )

            // 성공 후 폼 초기화
            setFormData({
                logCt: '',
                uploadedFile: null,
            })

            // 다이얼로그 닫기
            closeDialog('actionHold')

            // 현재 페이지로 부드럽게 재이동 (깜박임 없이)
            navigate(`/defect-management/details/${data.content[0].defectId}`, {
                replace: true,
            })
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
            // FormData 객체 생성 (파일 업로드를 위해 필요)
            const formDataToSend = new FormData()

            // JSON 데이터를 FormData에 추가
            const requestData = {
                defectId: data.content[0].defectId,
                statusCd: 'DS3005', // TO DO 처리 상태 코드
                logTitle: 'To Do 처리 되었습니다.',
                logCt: formData.logCt,
                createdBy: user.userId,
            }


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
            }
            // FormData 내용 확인
            for (let pair of formDataToSend.entries()) {
                console.log('FormData:', pair[0], pair[1])
            }

            await ApiService.post('/defectLogs/save', formDataToSend, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })

            toast.push(
                <Notification title={'TO DO 처리'} type="info">
                    TO DO 처리되었습니다.
                </Notification>,
            )
            // 성공 후 폼 초기화
            setFormData({
                logCt: '',
                uploadedFile: null,
            })

            // 다이얼로그 닫기
            closeDialog('todoProcess')

            // 현재 페이지로 부드럽게 재이동 (깜박임 없이)
            navigate(`/defect-management/details/${data.content[0].defectId}`, {
                replace: true,
            })
        } catch (error) {
            toast.push(
                <Notification title={'처리 실패'} type="danger">
                    {error.response?.data?.error ||
                        'TO DO 처리에 실패했습니다.'}
                </Notification>,
            )
        }
    }

    // TO DO 확정 처리 (새로 추가)
    const handleTodoConfirm = async () => {
        try {
            const formDataToSend = new FormData()

            const requestData = {
                defectId: data.content[0].defectId,
                statusCd: 'DS3006', // TO DO 확정 후 조치 대기 상태 코드
                logTitle: 'TO DO가 확정되어 조치 대기 상태로 변경되었습니다.',
                logCt: formData.logCt,
                createdBy: user.userId,
            }

            formDataToSend.append(
                'defectLogRequestDto',
                new Blob([JSON.stringify(requestData)], {
                    type: 'application/json',
                }),
            )

            if (formData.uploadedFile) {
                formDataToSend.append('files', formData.uploadedFile)
            }

            await ApiService.post('/defectLogs/save', formDataToSend, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })

            toast.push(
                <Notification title={'TO DO 확정'} type="success">
                    TO DO가 확정되어 조치 대기 상태로 변경되었습니다.
                </Notification>,
            )

            // 성공 후 폼 초기화
            setFormData({
                logCt: '',
                uploadedFile: null,
            })

            // 다이얼로그 닫기
            closeDialog('todoConfirm')

            navigate(`/defect-management/details/${data.content[0].defectId}`, {
                replace: true,
            })
        } catch (error) {
            console.error('Error:', error)
            toast.push(
                <Notification title={'처리 실패'} type="danger">
                    {error.response?.data?.error ||
                        'TO DO 확정 처리에 실패했습니다.'}
                </Notification>,
            )
        }
    }

    // 결함 종료 처리
    const handleDefectClose = async () => {
        try {
            const formDataToSend = new FormData()

            const requestData = {
                defectId: data.content[0].defectId,
                statusCd: 'DS5000', // 결함종료 상태 코드를 DS5000으로 수정
                logTitle: '결함이 종료되었습니다.',
                logCt: formData.logCt || '결함 종료 처리',
                createdBy: user.userId,
            }

            formDataToSend.append(
                'defectLogRequestDto',
                new Blob([JSON.stringify(requestData)], {
                    type: 'application/json',
                }),
            )

            if (formData.uploadedFile) {
                formDataToSend.append('files', formData.uploadedFile)
            }

            await ApiService.post('/defectLogs/save', formDataToSend, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })

            toast.push(
                <Notification title={'결함 종료'} type="success">
                    결함이 종료되었습니다.
                </Notification>,
            )

            // 성공 후 폼 초기화
            setFormData({
                logCt: '',
                uploadedFile: null,
            })

            // 다이얼로그 닫기
            closeDialog('defectClose')

            navigate(`/defect-management/details/${data.content[0].defectId}`, {
                replace: true,
            })
        } catch (error) {
            console.error('Error:', error)
            toast.push(
                <Notification title={'처리 실패'} type="danger">
                    {error.response?.data?.error ||
                        '결함 종료 처리에 실패했습니다.'}
                </Notification>,
            )
        }
    }

    // 결함 해제 처리
    const handleDefectRelease = async () => {
        try {
            const formDataToSend = new FormData()

            const requestData = {
                defectId: data.content[0].defectId,
                statusCd: 'DS6000', // 결함 해제 시 초기 상태로 변경
                logTitle: '결함 해제 처리 되었습니다.',
                logCt: formData.logCt,
                createdBy: user.userId,
            }

            formDataToSend.append(
                'defectLogRequestDto',
                new Blob([JSON.stringify(requestData)], {
                    type: 'application/json',
                }),
            )

            if (formData.uploadedFile) {
                formDataToSend.append('files', formData.uploadedFile)
            }

            await ApiService.post('/defectLogs/save', formDataToSend, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })

            toast.push(
                <Notification title={'결함 해제'} type="success">
                    결함이 해제되었습니다.
                </Notification>,
            )

            // 성공 후 폼 초기화
            setFormData({
                logCt: '',
                uploadedFile: null,
            })

            // 다이얼로그 닫기
            closeDialog('defectRelease')

            navigate('/defect-management/in-progress')
        } catch (error) {
            console.error('Error:', error)
            toast.push(
                <Notification title={'처리 실패'} type="danger">
                    {error.response?.data?.error ||
                        '결함 해제 처리에 실패했습니다.'}
                </Notification>,
            )
        }
    }

    // 결함 재발생 처리
    const handleDefectReoccurrence = async () => {
        try {
            const formDataToSend = new FormData()

            const requestData = {
                defectId: data.content[0].defectId,
                statusCd: 'DS4002', // 결함 재발생 시 진행 상태로 변경
                logTitle: '결함 재발생 처리 되었습니다.',
                logCt: formData.logCt,
                createdBy: user.userId,
            }

            formDataToSend.append(
                'defectLogRequestDto',
                new Blob([JSON.stringify(requestData)], {
                    type: 'application/json',
                }),
            )

            if (formData.uploadedFile) {
                formDataToSend.append('files', formData.uploadedFile)
            }

            await ApiService.post('/defectLogs/save', formDataToSend, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })

            toast.push(
                <Notification title={'결함 재발생'} type="warning">
                    결함이 재발생 처리되었습니다.
                </Notification>,
            )

            // 성공 후 폼 초기화
            setFormData({
                logCt: '',
                uploadedFile: null,
            })

            // 다이얼로그 닫기
            closeDialog('defectReoccurrence')

            navigate(`/defect-management/details/${data.content[0].defectId}`, {
                replace: true,
            })
        } catch (error) {
            console.error('Error:', error)
            toast.push(
                <Notification title={'처리 실패'} type="danger">
                    {error.response?.data?.error ||
                        '결함 재발생 처리에 실패했습니다.'}
                </Notification>,
            )
        }
    }

    // 결함조치 반려 처리
    const handleDefectReject = async () => {
        try {
            const formDataToSend = new FormData()

            const requestData = {
                defectId: data.content[0].defectId,
                statusCd: 'DS4001',
                logTitle: '결함조치가 반려되었습니다.',
                logCt: formData.logCt,
                createdBy: user.userId,
            }

            formDataToSend.append(
                'defectLogRequestDto',
                new Blob([JSON.stringify(requestData)], {
                    type: 'application/json',
                }),
            )

            if (formData.uploadedFile) {
                formDataToSend.append('files', formData.uploadedFile)
            }

            await ApiService.post('/defectLogs/save', formDataToSend, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })

            toast.push(
                <Notification title={'조치 반려'} type="warning">
                    결함조치가 반려되었습니다.
                </Notification>,
            )

            // 성공 후 폼 초기화
            setFormData({
                logCt: '',
                uploadedFile: null,
            })

            // 다이얼로그 닫기
            closeDialog('defectReject')

            navigate(`/defect-management/details/${data.content[0].defectId}`, {
                replace: true,
            })
        } catch (error) {
            console.error('Error:', error)
            toast.push(
                <Notification title={'처리 실패'} type="danger">
                    {error.response?.data?.error ||
                        '결함조치 반려 처리에 실패했습니다.'}
                </Notification>,
            )
        }
    }

    // 사용자 목록 가져오기
    const fetchUsers = async () => {
        setIsLoadingUsers(true)
        try {
            const response = await ApiService.get(
                '/projects/assignUserList',
                {},
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
            openDialog('defectTransfer')
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
            await ApiService.post('/defectLogs/save', formDataToSend, {
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

            // 다이얼로그 닫기
            closeDialog('defectTransfer')

            navigate('/defect-management/in-progress')

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

    // 현재 상태 확인
    const currentStatus = data.content?.[0]?.statusCd
    const isActionCompleted = currentStatus === 'DS3000'
    const isDefectClosed =
        currentStatus === 'DS5000' || currentStatus === 'DS6000'
    const isDefectRejected = currentStatus === 'DS4001'
    const isDefectHeld = currentStatus === 'DS4000'
    const isDefectTodo = currentStatus === 'DS3005'
    const isDefectTodoComplete = currentStatus === 'DS3006'

    // 세션 사용자와 결함 할당자 비교
    const isAssignedToCurrentUser =
        data.content?.[0]?.assignUserId === user.userId

    return (
        <div className="w-full min-h-screen space-y-4 bg-gray-50 pb-8">
            {/* 대제목 헤더 - 좌측 정렬 */}
            <div className="py-4 pl-6">
                <h1 className="text-3xl font-bold text-gray-900">
                    결함관리 ({data.content[0].defectId || 'N/A'})
                </h1>
            </div>

            {/* 처리 이력 영역 - 항상 표시 */}
            <div className="pl-6 pr-6 flex-1">
                <DefectTimeline
                    data={data}
                    logCt={formData.logCt}
                    uploadedFile={formData.uploadedFile}
                    onLogCtChange={handleLogCtChange}
                    onFileChange={handleFileChange}
                    hideAssignUser={isDefectClosed} // 결함 종료 시 담당자 숨김
                />
            </div>

            {/* DS5000 상태일 때 최종 완료 메시지 표시 */}
            {isDefectClosed && (
                <div className="flex justify-center">
                    <Button
                        className="text-base py-3 px-6"
                        icon={<HiOutlineArrowLeft />}
                        onClick={handleBackToList}
                    >
                        목록으로
                    </Button>
                </div>
            )}

            {/* 결함 이관 SelectBox 영역 - DS5000이 아닐 때만 표시 */}
            {!isDefectClosed && showTransferSelect && (
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
                                    onClick={handleDefectTransferClick}
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

            {/* 액션 버튼 영역 - DS5000일 때는 버튼 표시하지 않음 */}
            {!isDefectClosed && (
                <div className="pl-6 pr-6 py-4 mt-auto">
                    {/* 세션 사용자와 결함 할당자가 일치하지 않으면 목록으로 버튼만 표시 */}
                    {!isAssignedToCurrentUser ? (
                        <div className="flex justify-center">
                            <Button
                                className="text-base py-3 px-6"
                                icon={<HiOutlineArrowLeft />}
                                onClick={handleBackToList}
                            >
                                목록으로
                            </Button>
                        </div>
                    ) : (
                        <>
                            {isActionCompleted ? (
                                // DS3000(조치완료) 상태일 때 결함 종료와 결함조치 반려 버튼 표시
                                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-success hover:border-success hover:ring-1 ring-success hover:text-success'
                                        }
                                        icon={<HiOutlineCheckCircle />}
                                        onClick={() =>
                                            openDialog('defectClose')
                                        }
                                    >
                                        결함 종료
                                    </Button>
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-danger hover:border-danger hover:ring-1 ring-danger hover:text-danger'
                                        }
                                        icon={<HiOutlineX />}
                                        onClick={() =>
                                            openDialog('defectReject')
                                        }
                                    >
                                        결함조치 반려(조치 안됨)
                                    </Button>
                                    <Button
                                        className="w-full text-base py-3"
                                        icon={<HiOutlineArrowLeft />}
                                        onClick={handleBackToList}
                                    >
                                        목록으로
                                    </Button>
                                </div>
                            ) : isDefectTodo ? (
                                // DS3005(TO DO) 상태일 때 TO DO 확정, 결함재발생, 목록으로 버튼 표시
                                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-success hover:border-success hover:ring-1 ring-success hover:text-success'
                                        }
                                        icon={<HiOutlineCheck />}
                                        onClick={() =>
                                            openDialog('todoConfirm')
                                        }
                                    >
                                        TO-DO 확정(조치 대기)
                                    </Button>
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-warning hover:border-warning hover:ring-1 ring-warning hover:text-warning'
                                        }
                                        icon={<HiOutlineX />}
                                        onClick={() =>
                                            openDialog('defectReoccurrence')
                                        }
                                    >
                                        결함 재발생
                                    </Button>
                                    <Button
                                        className="w-full text-base py-3"
                                        icon={<HiOutlineArrowLeft />}
                                        onClick={handleBackToList}
                                    >
                                        목록으로
                                    </Button>
                                </div>
                            ) : isDefectTodoComplete ? (
                                // DS3006(TO DO 완료) 상태일 때 조치 완료와 목록으로 버튼 표시
                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-success hover:border-success hover:ring-1 ring-success hover:text-success'
                                        }
                                        icon={<HiOutlineCheck />}
                                        onClick={() =>
                                            openDialog('actionComplete')
                                        }
                                    >
                                        조치 완료
                                    </Button>
                                    <Button
                                        className="w-full text-base py-3"
                                        icon={<HiOutlineArrowLeft />}
                                        onClick={handleBackToList}
                                    >
                                        목록으로
                                    </Button>
                                </div>
                            ) : isDefectRejected ? (
                                // DS4001(결함조치 반려) 상태일 때 특별한 버튼들 표시
                                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-success hover:border-success hover:ring-1 ring-success hover:text-success'
                                        }
                                        icon={<HiOutlineCheck />}
                                        onClick={() =>
                                            openDialog('actionComplete')
                                        }
                                    >
                                        조치완료
                                    </Button>
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-warning hover:border-warning hover:ring-1 ring-warning hover:text-warning'
                                        }
                                        icon={<HiOutlinePause />}
                                        onClick={() => openDialog('actionHold')}
                                    >
                                        조치보류(결함아님)
                                    </Button>
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-info hover:border-info hover:ring-1 ring-info hover:text-info'
                                        }
                                        icon={<HiOutlineClipboardList />}
                                        onClick={() =>
                                            openDialog('todoProcess')
                                        }
                                    >
                                        TO DO 처리
                                    </Button>
                                    <Button
                                        className="w-full text-base py-3"
                                        icon={<HiOutlineArrowLeft />}
                                        onClick={handleBackToList}
                                    >
                                        목록으로
                                    </Button>
                                </div>
                            ) : isDefectHeld ? (
                                // DS4000(조치보류) 상태일 때 특별한 버튼들 표시
                                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-primary hover:border-primary hover:ring-1 ring-primary hover:text-primary'
                                        }
                                        icon={<HiOutlineCheck />}
                                        onClick={() =>
                                            openDialog('defectRelease')
                                        }
                                    >
                                        결함 해제
                                    </Button>
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-warning hover:border-warning hover:ring-1 ring-warning hover:text-warning'
                                        }
                                        icon={<HiOutlineX />}
                                        onClick={() =>
                                            openDialog('defectReoccurrence')
                                        }
                                    >
                                        결함 재발생
                                    </Button>
                                    <Button
                                        className="w-full text-base py-3"
                                        icon={<HiOutlineArrowLeft />}
                                        onClick={handleBackToList}
                                    >
                                        목록으로
                                    </Button>
                                </div>
                            ) : (
                                // DS3000이 아닌 다른 상태일 때 기존 버튼들 표시
                                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-6">
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-success hover:border-success hover:ring-1 ring-success hover:text-success'
                                        }
                                        icon={<HiOutlineCheck />}
                                        onClick={() =>
                                            openDialog('actionComplete')
                                        }
                                    >
                                        조치완료
                                    </Button>
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-warning hover:border-warning hover:ring-1 ring-warning hover:text-warning'
                                        }
                                        icon={<HiOutlinePause />}
                                        onClick={() => openDialog('actionHold')}
                                    >
                                        조치보류(결함아님)
                                    </Button>
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-info hover:border-info hover:ring-1 ring-info hover:text-info'
                                        }
                                        icon={<HiOutlineClipboardList />}
                                        onClick={() =>
                                            openDialog('todoProcess')
                                        }
                                    >
                                        TO DO 처리
                                    </Button>
                                    <Button
                                        className="w-full text-base py-3"
                                        customColorClass={() =>
                                            'text-indigo hover:border-indigo hover:ring-1 ring-indigo hover:text-indigo'
                                        }
                                        icon={<HiOutlineShare />}
                                        onClick={handleDefectTransferClick}
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
                            )}
                        </>
                    )}
                </div>
            )}

            {/* 확인 다이얼로그들 */}
            {/* 조치완료 확인 다이얼로그 */}
            <ConfirmDialog
                isOpen={dialogStates.actionComplete}
                title="조치 완료"
                onClose={() => closeDialog('actionComplete')}
                onRequestClose={() => closeDialog('actionComplete')}
                onCancel={() => closeDialog('actionComplete')}
                onConfirm={handleActionComplete}
                confirmText={'저장'}
            >
                결함 조치를 완료하시겠습니까?
            </ConfirmDialog>

            {/* 조치보류 확인 다이얼로그 */}
            <ConfirmDialog
                isOpen={dialogStates.actionHold}
                title="조치 보류"
                onClose={() => closeDialog('actionHold')}
                onRequestClose={() => closeDialog('actionHold')}
                onCancel={() => closeDialog('actionHold')}
                onConfirm={handleActionHold}
                confirmText={'저장'}
            >
                결함 조치를 보류하시겠습니까?
            </ConfirmDialog>

            {/* TO DO 처리 확인 다이얼로그 */}
            <ConfirmDialog
                isOpen={dialogStates.todoProcess}
                title="TO DO 처리"
                onClose={() => closeDialog('todoProcess')}
                onRequestClose={() => closeDialog('todoProcess')}
                onCancel={() => closeDialog('todoProcess')}
                onConfirm={handleTodoProcess}
                confirmText={'저장'}
            >
                TO DO로 처리하시겠습니까?
            </ConfirmDialog>

            {/* TO DO 확정 확인 다이얼로그 */}
            <ConfirmDialog
                isOpen={dialogStates.todoConfirm}
                title="TO DO 확정"
                onClose={() => closeDialog('todoConfirm')}
                onRequestClose={() => closeDialog('todoConfirm')}
                onCancel={() => closeDialog('todoConfirm')}
                onConfirm={handleTodoConfirm}
                confirmText={'저장'}
            >
                TO DO를 확정하시겠습니까?
            </ConfirmDialog>

            {/* 결함 종료 확인 다이얼로그 */}
            <ConfirmDialog
                isOpen={dialogStates.defectClose}
                title="결함 종료"
                onClose={() => closeDialog('defectClose')}
                onRequestClose={() => closeDialog('defectClose')}
                onCancel={() => closeDialog('defectClose')}
                onConfirm={handleDefectClose}
                confirmText={'저장'}
            >
                결함을 종료하시겠습니까?
            </ConfirmDialog>

            {/* 결함 해제 확인 다이얼로그 */}
            <ConfirmDialog
                isOpen={dialogStates.defectRelease}
                title="결함 해제"
                onClose={() => closeDialog('defectRelease')}
                onRequestClose={() => closeDialog('defectRelease')}
                onCancel={() => closeDialog('defectRelease')}
                onConfirm={handleDefectRelease}
                confirmText={'저장'}
            >
                결함을 해제하시겠습니까?
            </ConfirmDialog>

            {/* 결함 재발생 확인 다이얼로그 */}
            <ConfirmDialog
                isOpen={dialogStates.defectReoccurrence}
                title="결함 재발생"
                onClose={() => closeDialog('defectReoccurrence')}
                onRequestClose={() => closeDialog('defectReoccurrence')}
                onCancel={() => closeDialog('defectReoccurrence')}
                onConfirm={handleDefectReoccurrence}
                confirmText={'저장'}
            >
                결함을 재발생 처리하시겠습니까?
            </ConfirmDialog>

            {/* 결함조치 반려 확인 다이얼로그 */}
            <ConfirmDialog
                isOpen={dialogStates.defectReject}
                title="결함조치 반려"
                onClose={() => closeDialog('defectReject')}
                onRequestClose={() => closeDialog('defectReject')}
                onCancel={() => closeDialog('defectReject')}
                onConfirm={handleDefectReject}
                confirmText={'저장'}
            >
                결함 조치를 반려하시겠습니까?
            </ConfirmDialog>

            {/* 결함 이관 확인 다이얼로그 */}
            <ConfirmDialog
                isOpen={dialogStates.defectTransfer}
                title="결함 이관"
                onClose={() => closeDialog('defectTransfer')}
                onRequestClose={() => closeDialog('defectTransfer')}
                onCancel={() => closeDialog('defectTransfer')}
                onConfirm={handleDefectTransfer}
                confirmText={'저장'}
            >
                {selectedUser &&
                    `${selectedUser.label}님에게 결함을 이관하시겠습니까?`}
            </ConfirmDialog>
        </div>
    )
}

export default DefectSection
