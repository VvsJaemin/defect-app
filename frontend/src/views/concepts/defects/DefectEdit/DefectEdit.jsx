
import { useEffect, useState } from 'react'
import Card from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Notification from '@/components/ui/Notification'
import toast from '@/components/ui/toast'
import ConfirmDialog from '@/components/shared/ConfirmDialog'
import Input from '@/components/ui/Input'
import Select from '@/components/ui/Select'
import Upload from '@/components/ui/Upload'
import { HiOutlineArrowLeft, HiSave, HiTrash } from 'react-icons/hi'
import { FcImageFile } from 'react-icons/fc'
import { useNavigate, useParams } from 'react-router'
import { apiPrefix } from '@/configs/endpoint.config.js'
import axios from 'axios'
import useSWR from 'swr'
import Textarea from '@/views/ui-components/forms/Input/Textarea.jsx'
import { useAuth } from '@/auth/index.js'

const DefectEdit = () => {
    const { defectId } = useParams()
    const navigate = useNavigate()

    const { user } = useAuth()

    // 상태 변수들 선언
    const [saveDialogOpen, setSaveDialogOpen] = useState(false)
    const [alertDialogOpen, setAlertDialogOpen] = useState(false)
    const [alertMessage, setAlertMessage] = useState('')
    const [alertTitle, setAlertTitle] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [userOptions, setUserOptions] = useState([])
    const [projectOptions, setProjectOptions] = useState([])
    const [uploadedFiles, setUploadedFiles] = useState([])
    const [existingFiles, setExistingFiles] = useState([])

    // DefectRequestDto에 맞게 formData 구조 수정
    const [formData, setFormData] = useState({
        defectId: '', // 결함 ID (수정용)
        projectId: '', // 프로젝트 ID
        assigneeId: '', // 담당자 ID
        statusCode: '', // 상태 코드
        seriousCode: '', // 심각도 코드
        orderCode: '', // 순서 코드 (우선순위)
        defectDivCode: '', // 결함 분류 코드
        defectTitle: '', // 결함 제목
        defectMenuTitle: '', // 결함 메뉴 제목
        defectUrlInfo: '', // 결함 URL 정보
        defectContent: '', // 결함 내용
        defectEtcContent: '', // 기타 내용
        createdBy: '', // 기타 내용
        openYn: 'Y', // 공개 여부
    })

    const defectSeriousOptions = [
        { value: '', label: '선택하세요' },
        { value: '1', label: '영향없음' },
        { value: '2', label: '낮음' },
        { value: '3', label: '보통' },
        { value: '4', label: '높음' },
        { value: '5', label: '치명적' },
    ]

    const priorityOptions = [
        { value: '', label: '선택하세요' },
        { value: '1', label: '낮음' },
        { value: '2', label: '보통' },
        { value: '3', label: '높음' },
        { value: '4', label: '긴급' },
        { value: '5', label: '최우선' },
    ]

    const defectCategoryOptions = [
        { value: '', label: '선택하세요' },
        { value: 'SYSTEM', label: '시스템결함' },
        { value: 'FUNCTION', label: '기능결함' },
        { value: 'DOCUMENT', label: '문서결함' },
        { value: 'IMPROVING', label: '개선권고' },
        { value: 'NEW', label: '신규요청' },
        { value: 'UI', label: 'UI결함' },
    ]

    // 결함 정보 로드 - 컨트롤러 API 엔드포인트에 맞게 수정
    const { data, isLoading, error } = useSWR(
        defectId ? `/defects/read/${defectId}` : null,
        (url) =>
            axios
                .get(`${apiPrefix}${url}`, {
                    withCredentials: true,
                })
                .then((res) => res.data),
        {
            revalidateOnFocus: false,
            revalidateIfStale: false,
            revalidateOnMount: true,
        },
    )

    // 담당자 수정 권한 확인
    const canEditAssignee = user?.userId === data?.createdBy

    // 할당 가능한 사용자 목록 가져오기
    useEffect(() => {
        const fetchUsers = async () => {
            try {
                const response = await axios.get(
                    `${apiPrefix}/projects/assignUserList`,
                    {
                        withCredentials: true,
                    },
                )

                const users = response.data.map((user) => ({
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
            }
        }

        const fetchDefectProjects = async () => {
            try {
                const response = await axios.get(
                    `${apiPrefix}/defects/projectList`,
                    {
                        withCredentials: true,
                    },
                )

                const projects = response.data.map((project) => ({
                    value: project.projectId,
                    label: project.projectName,
                }))

                setProjectOptions(projects)
            } catch (error) {
                console.error('프로젝트 목록을 가져오는 중 오류 발생:', error)
                toast.push(
                    <Notification title={'데이터 로드 실패'} type="warning">
                        프로젝트 목록을 가져오는 중 오류가 발생했습니다.
                    </Notification>,
                )
            }
        }

        fetchUsers()
        fetchDefectProjects()
    }, [])

    // 데이터가 로드되면 폼 데이터 설정
    useEffect(() => {
        if (data) {
            setFormData({
                defectId: data.defectId || '',
                projectId: data.projectId || '',
                assigneeId: data.assigneeId || '',
                statusCode: data.statusCode || '',
                seriousCode: data.seriousCode || '',
                orderCode: data.orderCode || '',
                defectDivCode: data.defectDivCode || '',
                defectTitle: data.defectTitle || '',
                defectMenuTitle: data.defectMenuTitle || '',
                defectUrlInfo: data.defectUrlInfo || '',
                defectContent: data.defectContent || '',
                defectEtcContent: data.defectEtcContent || '',
                openYn: data.openYn || 'Y',
            })

            // 기존 첨부파일이 있다면 설정
            if (data.attachmentFiles) {
                setExistingFiles(data.attachmentFiles)
            }
        }
    }, [data])

    if (isLoading) {
        return (
            <div className="w-full p-5">
                <div className="text-center">데이터를 불러오는 중입니다...</div>
            </div>
        )
    }

    if (error) {
        return (
            <div className="w-full p-5">
                <div className="text-center text-red-500">
                    결함 정보를 불러오는 중 오류가 발생했습니다. 다시 시도해
                    주세요.
                </div>
            </div>
        )
    }

    // 파일 업로드 처리 함수
    const handleFileUpload = (files) => {
        const newFiles = Array.from(files)

        setUploadedFiles((prev) => [...prev, ...newFiles])
    }

    const handleFileRemove = (remainingFiles) => {
        setUploadedFiles(remainingFiles)
    }

    // 기존 파일 삭제 처리
    const handleExistingFileRemove = (logSeq) => {
        setExistingFiles((prev) =>
            prev.filter((file) => file.logSeq !== logSeq),
        )
    }


    // 파일 다운로드 처리 함수 추가 (handleExistingFileRemove 함수 근처에 추가)
    const handleFileDownload = async (file) => {
        try {
            console.log('file:', file)
            const response = await axios.get(
                `${apiPrefix}/files/download/${file.sysFileName}`,
                {
                    responseType: 'blob',
                    withCredentials: true,
                    headers: {
                        Accept: 'application/octet-stream',
                    },
                },
            )

            // 파일 다운로드 처리
            const url = window.URL.createObjectURL(new Blob([response.data]))
            const link = document.createElement('a')
            link.href = url
            link.setAttribute('download', file.orgFileName)
            document.body.appendChild(link)
            link.click()
            link.remove()
            window.URL.revokeObjectURL(url)
        } catch (error) {
            console.error('파일 다운로드 중 오류 발생:', error)
            toast.push(
                <Notification title={'다운로드 실패'} type="warning">
                    파일 다운로드 중 오류가 발생했습니다.
                </Notification>,
            )
        }
    }

    const handleInputChange = (e) => {
        const { name, value } = e.target
        setFormData((prev) => ({
            ...prev,
            [name]: value,
        }))
    }

    // 심각도 선택 변경 처리
    const handleSeriousChange = (selectedOption) => {
        if (selectedOption) {
            setFormData((prev) => ({
                ...prev,
                seriousCode: selectedOption.value,
            }))
        }
    }

    // 우선순위(순서코드) 선택 변경 처리
    const handleOrderChange = (selectedOption) => {
        if (selectedOption) {
            setFormData((prev) => ({
                ...prev,
                orderCode: selectedOption.value,
            }))
        }
    }

    // 결함 분류 선택 변경 처리
    const handleCategoryChange = (selectedOption) => {
        if (selectedOption) {
            setFormData((prev) => ({
                ...prev,
                defectDivCode: selectedOption.value,
            }))
        }
    }

    // 프로젝트 선택 변경 처리
    const handleProjectChange = (selectedOption) => {
        if (selectedOption) {
            setFormData((prev) => ({
                ...prev,
                projectId: selectedOption.value,
            }))
        }
    }

    // 담당자 선택 변경 처리
    const handleAssigneeChange = (selectedOption) => {
        // 권한이 없으면 변경을 허용하지 않음
        if (!canEditAssignee) {
            showAlert('권한 없음', '결함 등록자만 담당자를 변경할 수 있습니다.')
            return
        }

        if (selectedOption) {
            setFormData((prev) => ({
                ...prev,
                assigneeId: selectedOption.value,
            }))
        }
    }

    const handleBackToList = () => {
        navigate('/defect-management')
    }

    // 경고창 닫기
    const handleAlertClose = () => {
        setAlertDialogOpen(false)
    }

    // 경고창 표시 함수
    const showAlert = (title, message) => {
        setAlertTitle(title)
        setAlertMessage(message)
        setAlertDialogOpen(true)
    }

    // 저장 다이얼로그 관련 함수
    const handleSaveDialogClose = () => {
        setSaveDialogOpen(false)
    }

    const handleSaveDialogOpen = (e) => {
        e.preventDefault() // 폼 제출 방지

        // 필수 필드 검증
        if (!formData.projectId) {
            showAlert('프로젝트 미선택', '프로젝트를 선택해주세요.')
            return
        }

        if (!formData.assigneeId) {
            showAlert('담당자 미선택', '담당자를 선택해주세요.')
            return
        }

        if (!formData.defectTitle) {
            showAlert('결함 제목 미입력', '결함 제목을 입력해주세요.')
            return
        }

        if (!formData.defectContent) {
            showAlert('결함 상세 미입력', '결함 상세 내용을 입력해주세요.')
            return
        }

        // 모든 검증을 통과하면 저장 다이얼로그 열기
        setSaveDialogOpen(true)
    }

    // 실제 저장 처리 함수
    const handleSave = async () => {
        try {
            setIsSubmitting(true)

            // FormData 객체 생성 (파일 업로드를 위해)
            const formDataToSend = new FormData()


            const deletedFiles = data.attachmentFiles
                ? data.attachmentFiles
                    .filter(
                        (originalFile) =>
                            !existingFiles.some(
                                (currentFile) =>
                                    currentFile.logSeq === originalFile.logSeq,
                            ),
                    )
                : []
            const indices = deletedFiles.map(file => file.logSeq.split('_')[1])


            const requestData = {
                defectId: formData.defectId,
                projectId: formData.projectId,
                assigneeId: formData.assigneeId,
                statusCode: formData.statusCode,
                seriousCode: formData.seriousCode,
                orderCode: formData.orderCode,
                defectDivCode: formData.defectDivCode,
                defectTitle: formData.defectTitle,
                defectMenuTitle: formData.defectMenuTitle,
                defectUrlInfo: formData.defectUrlInfo,
                defectContent: formData.defectContent,
                defectEtcContent: formData.defectEtcContent,
                openYn: formData.openYn,
                // 삭제된 기존 파일 ID 목록 (원본 데이터와 현재 상태 비교)
                logSeq: indices,

            }

            // JSON 데이터를 Blob으로 추가
            formDataToSend.append(
                'defectRequestDto',
                new Blob([JSON.stringify(requestData)], {
                    type: 'application/json',
                }),
            )

            // 새로 추가된 파일들을 FormData에 추가
            if (uploadedFiles && uploadedFiles.length > 0) {
                uploadedFiles.forEach((file) => {
                    formDataToSend.append('files', file)
                })
            }

            // 서버에 결함 수정 요청
            await axios.put(
                `${apiPrefix}/defects/modify-defects`,
                formDataToSend,
                {
                    headers: {
                        'Content-Type': 'multipart/form-data',
                    },
                    withCredentials: true,
                },
            )

            toast.push(
                <Notification title={'수정 성공'} type="success">
                    결함이 성공적으로 수정되었습니다
                </Notification>,
            )

            // 결함 관리 페이지로 이동
            navigate('/defect-management')
        } catch (error) {
            toast.push(
                <Notification title={'수정 실패'} type="warning">
                    {error.response?.data?.error ||
                        '처리중 오류가 발생되었습니다.'}
                </Notification>,
            )
        } finally {
            setIsSubmitting(false)
            setSaveDialogOpen(false)
        }
    }

    return (
        <Card className="w-full">
            <form onSubmit={handleSaveDialogOpen}>
                <div className="flex justify-between items-center mb-4">
                    <h4 className="font-bold">결함 수정</h4>
                    <div className="flex gap-2">
                        <Button
                            type="button"
                            icon={<HiOutlineArrowLeft />}
                            onClick={handleBackToList}
                        >
                            취소
                        </Button>
                        <Button
                            type="submit"
                            variant="solid"
                            icon={<HiSave />}
                            loading={isSubmitting}
                        >
                            수정
                        </Button>
                    </div>
                </div>

                <div className="flex flex-col xl:justify-between h-full 2xl:min-w-[360px] mx-auto">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-7 mt-10">
                        <div>
                            <label className="font-semibold block mb-2">
                                사이트 / 프로젝트명
                            </label>
                            <Select
                                options={[
                                    { value: '', label: '선택하세요' },
                                    ...projectOptions,
                                ]}
                                value={
                                    [
                                        {
                                            value: '',
                                            label: '선택하세요',
                                        },
                                        ...projectOptions,
                                    ].find(
                                        (option) =>
                                            option.value === formData.projectId,
                                    ) || null
                                }
                                onChange={handleProjectChange}
                                placeholder="프로젝트 선택"
                                isSearchable={false}
                                openMenuOnFocus={true}
                                isClearable={false}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                담당자
                                {!canEditAssignee}
                            </label>
                            <Select
                                options={[
                                    { value: '', label: '선택하세요' },
                                    ...userOptions,
                                ]}
                                value={
                                    [
                                        {
                                            value: '',
                                            label: '선택하세요',
                                        },
                                        ...userOptions,
                                    ].find(
                                        (option) =>
                                            option.value ===
                                            formData.assigneeId,
                                    ) || null
                                }
                                onChange={handleAssigneeChange}
                                placeholder="담당자 선택"
                                isSearchable={false}
                                openMenuOnFocus={true}
                                isClearable={false}
                                isDisabled={!canEditAssignee}
                                className={!canEditAssignee ? 'opacity-60' : ''}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                중요도
                            </label>
                            <Select
                                options={defectSeriousOptions}
                                value={
                                    defectSeriousOptions.find(
                                        (option) =>
                                            option.value ===
                                            formData.seriousCode,
                                    ) || null
                                }
                                onChange={handleSeriousChange}
                                placeholder="중요도 선택"
                                isSearchable={false}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                우선순위
                            </label>
                            <Select
                                options={priorityOptions}
                                value={
                                    priorityOptions.find(
                                        (option) =>
                                            option.value === formData.orderCode,
                                    ) || null
                                }
                                onChange={handleOrderChange}
                                placeholder="우선순위 선택"
                                isSearchable={false}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                결함 분류
                            </label>
                            <Select
                                options={defectCategoryOptions}
                                value={
                                    defectCategoryOptions.find(
                                        (option) =>
                                            option.value ===
                                            formData.defectDivCode,
                                    ) || null
                                }
                                onChange={handleCategoryChange}
                                placeholder="결함 분류 선택"
                                isSearchable={false}
                            />
                        </div>

                        <div className="md:col-span-2">
                            <label className="font-semibold block mb-2">
                                결함 요약(제목)
                            </label>
                            <Input
                                type="text"
                                name="defectTitle"
                                value={formData.defectTitle}
                                onChange={handleInputChange}
                                placeholder="결함 요약(제목)"
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                결함 발생 메뉴
                            </label>
                            <Input
                                type="text"
                                name="defectMenuTitle"
                                value={formData.defectMenuTitle}
                                onChange={handleInputChange}
                                placeholder="결함 발생 메뉴"
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                결함 발생 URL
                            </label>
                            <Input
                                type="text"
                                name="defectUrlInfo"
                                value={formData.defectUrlInfo}
                                onChange={handleInputChange}
                                placeholder="결함 발생 URL"
                            />
                        </div>

                        <div className="md:col-span-2">
                            <label className="font-semibold block mb-2">
                                결함 상세(설명)
                            </label>
                            <Textarea
                                name="defectContent"
                                value={formData.defectContent}
                                onChange={handleInputChange}
                                placeholder="결함상세 설명 입력"
                            />
                        </div>

                        {/* 기존 파일 목록 */}
                        {existingFiles.length > 0 && (
                            <div className="md:col-span-2">
                                <label className="font-semibold block mb-2">
                                    기존 첨부 파일
                                </label>
                                <div className="space-y-2">
                                    {existingFiles.map((file, index) => (
                                        <div
                                            key={
                                                file.fileId ||
                                                `existing-file-${index}`
                                            }
                                            className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700 rounded-lg"
                                        >
                                            <div className="flex items-center space-x-3">
                                                <FcImageFile className="text-lg" />
                                                <div>
                                                    <button
                                                        type="button"
                                                        onClick={() =>
                                                            handleFileDownload(file)
                                                        }
                                                        className="text-sm font-medium text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 underline cursor-pointer"
                                                    >
                                                        {file.orgFileName}
                                                    </button>
                                                    {file.fileSize && (
                                                        <p className="text-xs text-gray-500 dark:text-gray-400">
                                                            {Math.round(
                                                                file.fileSize /
                                                                1024,
                                                            )}{' '}
                                                            KB
                                                        </p>
                                                    )}
                                                </div>
                                            </div>
                                            <div className="flex items-center space-x-2">
                                                <Button
                                                    type="button"
                                                    size="xs"
                                                    variant="solid"
                                                    color="red-600"
                                                    icon={<HiTrash />}
                                                    onClick={() =>
                                                        handleExistingFileRemove(
                                                            file.logSeq,
                                                        )
                                                    }
                                                >
                                                    삭제
                                                </Button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* 파일 업로드 섹션 */}
                        <div className="md:col-span-2">
                            <div className="flex items-center justify-between mb-2">
                                <label className="font-semibold">
                                    파일 업로드 (최대 3개)
                                </label>
                            </div>
                            <div className="space-y-4">
                                <div>
                                    <Upload
                                        draggable
                                        multiple
                                        onChange={handleFileUpload}
                                        onFileRemove={handleFileRemove}
                                        uploadLimit={
                                            3 - (existingFiles.length - 1)
                                        }
                                        accept=".jpeg,.jpg,.png,.gif,.pdf,.doc,.docx"
                                    >
                                        <div className="my-16 text-center">
                                            <div className="text-6xl mb-4 flex justify-center">
                                                <FcImageFile />
                                            </div>
                                            <p className="font-semibold">
                                                <span className="text-gray-800 dark:text-white">
                                                    파일을 여기에 드롭하거나{' '}
                                                </span>
                                                <span className="text-blue-500">
                                                    찾아보기
                                                </span>
                                            </p>
                                            <p className="mt-1 opacity-60 dark:text-white">
                                                지원 형식: jpeg, png, gif, pdf,
                                                doc, docx
                                            </p>
                                        </div>
                                    </Upload>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* 저장 확인 다이얼로그 */}
                <ConfirmDialog
                    isOpen={saveDialogOpen}
                    title="결함 수정"
                    onClose={handleSaveDialogClose}
                    onRequestClose={handleSaveDialogClose}
                    onCancel={handleSaveDialogClose}
                    onConfirm={handleSave}
                    confirmText={'수정'}
                >
                    <p>결함을 수정하시겠습니까?</p>
                </ConfirmDialog>

                {/* 경고 다이얼로그 - 알림 형태로 취소 버튼만 노출 */}
                <ConfirmDialog
                    type="warning"
                    isOpen={alertDialogOpen}
                    title={alertTitle}
                    onClose={handleAlertClose}
                    onRequestClose={handleAlertClose}
                    onCancel={handleAlertClose}
                    onConfirm={handleAlertClose}
                    confirmText={'확인'}
                    cancelButtonProps={{ style: { display: 'none' } }}
                >
                    <p>{alertMessage}</p>
                </ConfirmDialog>
            </form>
        </Card>
    )
}

export default DefectEdit