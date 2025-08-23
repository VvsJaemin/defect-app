import { useEffect, useMemo, useState } from 'react'
import Card from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Notification from '@/components/ui/Notification'
import toast from '@/components/ui/toast'
import ConfirmDialog from '@/components/shared/ConfirmDialog'
import Input from '@/components/ui/Input'
import Select from '@/components/ui/Select'
import { HiOutlineArrowLeft, HiSave } from 'react-icons/hi'
import { useNavigate, useParams } from 'react-router'
import useSWR from 'swr'
import ApiService from '@/services/ApiService.js'

const ProjectEdit = () => {
    const { projectId } = useParams()
    const navigate = useNavigate()

    // 상태 변수들 선언
    const [saveDialogOpen, setSaveDialogOpen] = useState(false)
    const [alertDialogOpen, setAlertDialogOpen] = useState(false)
    const [alertMessage, setAlertMessage] = useState('')
    const [alertTitle, setAlertTitle] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [formData, setFormData] = useState({
        projectName: '',
        urlInfo: '',
        statusCode: '',
        customerName: '',
        etcInfo: '',
        projAssignedUsers: [],
        assignedUsersMap: null,
    })

    // 프로젝트 상태 옵션 설정
    const statusOptions = [
        { value: '', label: '선택하세요' },
        { value: 'DEV', label: '개발버전' },
        { value: 'OPERATE', label: '운영버전' },
        { value: 'TEST', label: '테스트버전' },
    ]

    // 프로젝트 정보 로드
    const { data, isLoading, error } = useSWR(
        projectId ? ['/projects/read', { projectId }] : null,
        ([, params]) =>
            ApiService.get('/projects/read', {
                params,
            }).then((res) => res.data),
        {
            revalidateOnFocus: false,
            revalidateIStale: false,
            revalidateOnMount: true,
        },
    )

    // 할당 가능한 사용자 목록 가져오기
    const [availableUsers, setAvailableUsers] = useState([])

    useEffect(() => {
        const fetchUsers = async () => {
            try {
                const response = await ApiService.get(
                    '/projects/assignUserList',
                    {},
                )

                // API 응답에서 userName과 userId를 사용하여 옵션 배열 생성
                const users = response.data.map((user) => ({
                    value: user.userId,
                    label: user.userName,
                }))

                setAvailableUsers(users)
            } catch (error) {
                console.error('사용자 목록을 가져오는 중 오류 발생:', error)
                toast.push(
                    <Notification title={'데이터 로드 실패'} type="warning">
                        사용자 목록을 가져오는 중 오류가 발생했습니다.
                    </Notification>,
                )
            }
        }

        fetchUsers()
    }, [])

    // 데이터가 로드되면 폼 데이터 설정
    useEffect(() => {
        if (data) {
            // 서버에서 projAssignedUsers가 없을 경우 빈 배열로 초기화
            // assignedUsersMap이 있을 경우 키 값들을 projAssignedUsers로 사용
            const assignedUsers =
                data.projAssignedUsers ||
                (data.assignedUsersMap
                    ? Object.keys(data.assignedUsersMap)
                    : [])

            setFormData({
                projectId: data.projectId || '',
                projectName: data.projectName || '',
                urlInfo: data.urlInfo || '',
                statusCode: data.statusCode || '',
                customerName: data.customerName || '',
                etcInfo: data.etcInfo || '',
                projAssignedUsers: assignedUsers,
                assignedUsersMap: data.assignedUsersMap || {},
            })
        }
    }, [data])

    // 사용자 드롭다운에 표시할 현재 선택된 값들
    const selectedUserValues = useMemo(() => {
        // projAssignedUsers가 없거나 빈 배열이면 빈 배열 반환
        if (
            !formData.projAssignedUsers ||
            formData.projAssignedUsers.length === 0
        ) {
            return []
        }

        return formData.projAssignedUsers.map((userId) => {
            // 먼저 availableUsers에서 일치하는 항목 찾기
            const matched = availableUsers.find((user) => user.value === userId)
            if (matched) {
                return matched
            }

            // availableUsers에 없으면 기본 표시 생성
            // assignedUsersMap에서 이름을 가져오거나 없으면 userId 사용
            const label =
                formData.assignedUsersMap && formData.assignedUsersMap[userId]
                    ? formData.assignedUsersMap[userId]
                    : userId

            return {
                value: userId,
                label,
            }
        })
    }, [formData.projAssignedUsers, availableUsers, formData.assignedUsersMap])

    if (isLoading) {
        return (
            <></>
        )
    }

    if (error) {
        return (
            <div className="w-full p-5">
                <div className="text-center text-red-500">
                    프로젝트 정보를 불러오는 중 오류가 발생했습니다. 다시 시도해
                    주세요.
                </div>
            </div>
        )
    }

    const handleInputChange = (e) => {
        const { name, value } = e.target
        setFormData((prev) => ({
            ...prev,
            [name]: value,
        }))
    }

    const handleStatusChange = (selectedOption) => {
        if (selectedOption) {
            setFormData((prev) => ({
                ...prev,
                statusCode: selectedOption.value,
            }))
        }
    }

    // 멀티 셀렉트 변경 처리
    const handleMultiUserChange = (selectedOptions) => {
        if (selectedOptions && selectedOptions.length > 0) {
            // 선택된 사용자들의 value 값만 추출하여 배열로 저장
            const selectedUsers = selectedOptions.map((option) => option.value)

            setFormData((prev) => {
                const newFormData = {
                    ...prev,
                    projAssignedUsers: selectedUsers,
                }
                return newFormData
            })
        } else {
            // 선택된 항목이 없으면 빈 배열로 설정
            setFormData((prev) => {
                const newFormData = {
                    ...prev,
                    projAssignedUsers: [],
                }
                return newFormData
            })
        }
    }

    const handleBackToList = () => {
        navigate('/project-management')
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
        if (!formData.projectName) {
            showAlert('프로젝트명 미입력', '프로젝트명을 입력해주세요.')
            return
        }

        if (!formData.urlInfo) {
            showAlert('URL 미입력', 'URL을 입력해주세요.')
            return
        }

        // 상태 선택 여부 확인
        if (!formData.statusCode) {
            showAlert('프로젝트 상태 미선택', '프로젝트 상태를 선택해주세요.')
            return
        }

        if (!formData.customerName) {
            showAlert('고객사 미입력', '고객사를 입력해주세요.')
            return
        }

        // 할당 사용자 필수 선택 검증
        if (!formData.projAssignedUsers || formData.projAssignedUsers.length === 0) {
            showAlert('할당 사용자 미선택', '할당 사용자를 선택해주세요.')
            return
        }

        // 모든 검증을 통과하면 저장 다이얼로그 열기
        setSaveDialogOpen(true)
    }

    // 실제 저장 처리 함수
    const handleSave = async () => {
        try {
            setIsSubmitting(true)

            // 서버에 프로젝트 정보 업데이트 요청
            await ApiService.post(
                '/projects/modify-projects',
                {
                    projectId: formData.projectId,
                    projectName: formData.projectName,
                    urlInfo: formData.urlInfo,
                    customerName: formData.customerName,
                    statusCode: formData.statusCode,
                    etcInfo: formData.etcInfo,
                    projAssignedUsers: formData.projAssignedUsers || [],
                },
                {
                    headers: {
                        'Content-Type': 'application/json',
                    },
                },
            )

            toast.push(
                <Notification title={'수정 성공'} type="success">
                    프로젝트가 성공적으로 수정되었습니다.
                </Notification>,
            )

            // 프로젝트 관리 페이지로 이동
            navigate('/project-management')
        } catch (error) {
            toast.push(
                <Notification title={'수정 실패'} type="warning">
                    {error.response?.data?.error ||
                        '프로젝트 정보 수정이 실패했습니다.'}
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
                    <h4 className="font-bold">프로젝트 수정</h4>
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
                            저장
                        </Button>
                    </div>
                </div>

                <div className="flex flex-col xl:justify-between h-full max-w-none mx-auto">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-7 mt-10">
                        <div>
                            <label className="font-semibold block mb-2">
                                프로젝트 명<span className="text-red-500">*</span>
                            </label>
                            <Input
                                type="text"
                                name="projectName"
                                value={formData.projectName}
                                onChange={handleInputChange}
                                placeholder="프로젝트 명 입력"
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                URL<span className="text-red-500">*</span>
                            </label>
                            <Input
                                type="text"
                                name="urlInfo"
                                value={formData.urlInfo}
                                onChange={handleInputChange}
                                placeholder="URL 입력"
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                프로젝트 상태<span className="text-red-500">*</span>
                            </label>
                            <Select
                                options={statusOptions}
                                value={
                                    statusOptions.find(
                                        (option) =>
                                            option.value ===
                                            formData.statusCode,
                                    ) || null
                                }
                                onChange={handleStatusChange}
                                placeholder="프로젝트 상태 선택"
                                isSearchable={false}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                고객사<span className="text-red-500">*</span>
                            </label>
                            <Input
                                type="text"
                                name="customerName"
                                value={formData.customerName}
                                onChange={handleInputChange}
                                placeholder="고객사 입력"
                            />
                        </div>

                        <div className="md:col-span-2">
                            <label className="font-semibold block mb-2">
                                프로젝트 설명
                            </label>
                            <Input
                                type="text"
                                name="etcInfo"
                                value={formData.etcInfo}
                                onChange={handleInputChange}
                                placeholder="프로젝트 설명 입력"
                            />
                        </div>

                        <div className="md:col-span-1">
                            <label className="font-semibold block mb-2">
                                할당 사용자 <span className="text-red-500">*</span>
                            </label>
                            <Select
                                options={availableUsers}
                                value={selectedUserValues}
                                onChange={handleMultiUserChange}
                                placeholder="할당 사용자 선택"
                                isSearchable={false}
                                isMulti={true}
                                isClearable={true}
                                closeMenuOnSelect={false}
                            />
                        </div>
                    </div>
                </div>

                {/* 저장 확인 다이얼로그 */}
                <ConfirmDialog
                    isOpen={saveDialogOpen}
                    title="프로젝트 수정"
                    onClose={handleSaveDialogClose}
                    onRequestClose={handleSaveDialogClose}
                    onCancel={handleSaveDialogClose}
                    onConfirm={handleSave}
                    confirmText={'수정'}
                >
                    <p>프로젝트를 수정하시겠습니까?</p>
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

export default ProjectEdit