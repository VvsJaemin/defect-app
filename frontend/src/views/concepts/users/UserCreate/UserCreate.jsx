import { useState } from 'react'
import Card from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Avatar from '@/components/ui/Avatar/Avatar'
import ConfirmDialog from '@/components/shared/ConfirmDialog'
import Input from '@/components/ui/Input'
import Select from '@/components/ui/Select'
import { HiOutlineArrowLeft, HiSave } from 'react-icons/hi'
import { useNavigate } from 'react-router'
import { TbUser } from 'react-icons/tb'
import { apiPrefix } from '@/configs/endpoint.config.js'
import ApiService from '@/services/ApiService.js'

const UserCreate = () => {
    const navigate = useNavigate()

    // 상태 변수들 선언
    const [saveDialogOpen, setSaveDialogOpen] = useState(false)
    const [alertDialogOpen, setAlertDialogOpen] = useState(false)
    const [alertMessage, setAlertMessage] = useState('')
    const [alertTitle, setAlertTitle] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [errors, setErrors] = useState({
        userId: '',
        userName: '',
        userSeCd: '',
        password: '',
        confirmPassword: '',
    })
    const [formData, setFormData] = useState({
        userId: '',
        userName: '',
        userSeCd: '',
        password: '',
        confirmPassword: '',
    })

    // 권한 옵션 설정
    const roleOptions = [
        { value: 'CU', label: '고객사' },
        { value: 'DM', label: '결함검토/할당(dev manager)' },
        { value: 'DP', label: '결함처리(developer)' },
        { value: 'MG', label: '처리현황 조회(manager)' },
        { value: 'QA', label: '결함등록/완료(Q/A)' },
    ]

    const clearError = (fieldName) => {
        setErrors(prev => ({
            ...prev,
            [fieldName]: ''
        }))
    }

    const setError = (fieldName, message) => {
        setErrors(prev => ({
            ...prev,
            [fieldName]: message
        }))
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

    // 서버 에러 메시지를 깔끔하게 정리하는 함수
    const formatErrorMessage = (errorString) => {
        if (!errorString) return '사용자 등록 중 오류가 발생했습니다.'

        // 콤마로 구분된 에러 메시지들을 배열로 변환하고 중복 제거
        const errorMessages = errorString
            .split(',')
            .map(msg => msg.trim())
            .filter((msg, index, array) => array.indexOf(msg) === index) // 중복 제거
            .filter(msg => msg.length > 0) // 빈 문자열 제거
            .map(msg => `• ${msg}`) // 각 메시지 앞에 "• " 추가 (더 깔끔한 불릿)

        // 각 메시지를 새 줄로 연결
        return errorMessages.join('\n')
    }


    const handleInputChange = (e) => {
        const { name, value } = e.target
        setFormData((prev) => ({
            ...prev,
            [name]: value,
        }))

        // 입력 시 해당 필드의 에러 메시지 제거
        clearError(name)

        // 비밀번호 일치 여부 확인
        if (
            name === 'confirmPassword' ||
            (name === 'password' && formData.confirmPassword)
        ) {
            if (name === 'password' && value !== formData.confirmPassword) {
                setError('confirmPassword', '비밀번호가 일치하지 않습니다.')
            } else if (
                name === 'confirmPassword' &&
                value !== formData.password
            ) {
                setError('confirmPassword', '비밀번호가 일치하지 않습니다.')
            } else {
                clearError('confirmPassword')
            }
        }
    }

    const handleSelectChange = (selectedOption) => {
        if (selectedOption) {
            setFormData((prev) => ({
                ...prev,
                userSeCd: selectedOption.value,
            }))
            clearError('userSeCd')
        }
    }

    const handleBackToList = () => {
        navigate('/user-management')
    }

    // 저장 다이얼로그 관련 함수
    const handleSaveDialogClose = () => {
        setSaveDialogOpen(false)
    }

    const validateForm = () => {
        const newErrors = {}
        let isValid = true

        // 필수 필드 검증
        if (!formData.userId) {
            newErrors.userId = '사용자 ID를 입력해주세요.'
            isValid = false
        }

        if (!formData.userName) {
            newErrors.userName = '사용자명을 입력해주세요.'
            isValid = false
        }

        // 권한 선택 여부 확인
        if (!formData.userSeCd) {
            newErrors.userSeCd = '권한을 선택해주세요.'
            isValid = false
        }

        // 비밀번호 필드 검증
        if (!formData.password) {
            newErrors.password = '비밀번호를 입력해주세요.'
            isValid = false
        }

        // 비밀번호 확인 필드 검증
        if (!formData.confirmPassword) {
            newErrors.confirmPassword = '비밀번호 확인을 입력해주세요.'
            isValid = false
        } else if (formData.password && formData.confirmPassword && formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = '비밀번호가 일치하지 않습니다.'
            isValid = false
        }

        setErrors(newErrors)
        return isValid
    }

    const handleSaveDialogOpen = (e) => {
        e.preventDefault() // 폼 제출 방지

        if (validateForm()) {
            setSaveDialogOpen(true)
        }
    }

    // 실제 저장 처리 함수
    const handleSave = async () => {
        try {
            setIsSubmitting(true)

            // 서버에 사용자 등록 요청
            await ApiService.post(
                `${apiPrefix}/users/signup`,
                {
                    userId: formData.userId,
                    userName: formData.userName,
                    userSeCd: formData.userSeCd,
                    password: formData.password,
                },
                {
                    headers: {
                        'Content-Type': 'application/json',
                    },
                },
            )

            showAlert('등록 성공', '사용자가 성공적으로 등록되었습니다.')

            // 사용자 관리 페이지로 이동
            navigate('/user-management')
        } catch (error) {
            console.log(error)

            const errorMessage = formatErrorMessage(
                error.response?.data?.error ||
                error.response?.data?.message
            )

            showAlert('사용자 등록 오류', errorMessage)

            return false
        } finally {
            setIsSubmitting(false)
            setSaveDialogOpen(false)
        }
    }

    return (
        <Card className="w-full">
            <form onSubmit={handleSaveDialogOpen}>
                <div className="flex justify-between items-center mb-4">
                    <h4 className="font-bold">사용자 등록</h4>
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
                            등록
                        </Button>
                    </div>
                </div>

                <div className="flex flex-col xl:justify-between h-full 2xl:min-w-[360px] mx-auto">
                    <div className="flex xl:flex-col items-center gap-4 mt-6">
                        <Avatar size={90} shape="circle" icon={<TbUser />} />
                    </div>

                    <div className="grid grid-cols-1 gap-y-7 mt-10">
                        <div>
                            <label className="font-semibold block mb-2">
                                사용자 ID <span className="text-red-500">*</span>
                            </label>
                            <Input
                                type="text"
                                name="userId"
                                value={formData.userId}
                                onChange={handleInputChange}
                                placeholder={errors.userId || "사용자 ID 입력"}
                                invalid={!!errors.userId}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                사용자명 <span className="text-red-500">*</span>
                            </label>
                            <Input
                                type="text"
                                name="userName"
                                value={formData.userName}
                                onChange={handleInputChange}
                                placeholder={errors.userName || "사용자명 입력"}
                                invalid={!!errors.userName}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                권한 <span className="text-red-500">*</span>
                            </label>
                            <Select
                                options={roleOptions}
                                value={
                                    roleOptions.find(
                                        (option) =>
                                            option.value === formData.userSeCd,
                                    ) || null
                                }
                                onChange={handleSelectChange}
                                placeholder={errors.userSeCd || "권한 선택"}
                                isSearchable={false}
                                invalid={!!errors.userSeCd}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                비밀번호 <span className="text-red-500">*</span>
                            </label>
                            <Input
                                type="password"
                                name="password"
                                value={formData.password}
                                onChange={handleInputChange}
                                placeholder={errors.password || "비밀번호 입력"}
                                invalid={!!errors.password}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                비밀번호 확인 <span className="text-red-500">*</span>
                            </label>
                            <Input
                                type="password"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleInputChange}
                                placeholder={errors.confirmPassword || "비밀번호 확인"}
                                invalid={!!errors.confirmPassword}
                            />
                        </div>
                    </div>
                </div>

                {/* 저장 확인 다이얼로그 */}
                <ConfirmDialog
                    isOpen={saveDialogOpen}
                    title="사용자 등록"
                    onClose={handleSaveDialogClose}
                    onRequestClose={handleSaveDialogClose}
                    onCancel={handleSaveDialogClose}
                    onConfirm={handleSave}
                    confirmText={'등록'}
                >
                    <p>사용자를 등록하시겠습니까?</p>
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
                    <div style={{
                        textAlign: 'left',
                        whiteSpace: 'pre-line',
                        lineHeight: '1.6',
                        fontSize: '14px',
                        maxHeight: '300px',
                        overflowY: 'auto',
                        padding: '10px 0'
                    }}>
                        {alertMessage}
                    </div>
                </ConfirmDialog>

            </form>
        </Card>
    )
}

export default UserCreate