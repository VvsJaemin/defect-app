
import { useEffect, useState } from 'react'
import Card from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Avatar from '@/components/ui/Avatar/Avatar'
import Notification from '@/components/ui/Notification'
import toast from '@/components/ui/toast'
import ConfirmDialog from '@/components/shared/ConfirmDialog'
import Input from '@/components/ui/Input'
import Select from '@/components/ui/Select'
import { HiOutlineArrowLeft, HiSave } from 'react-icons/hi'
import { TbRefresh } from 'react-icons/tb'
import { useNavigate, useParams } from 'react-router'
import { TbUser } from 'react-icons/tb'
import { apiPrefix } from '@/configs/endpoint.config.js'
import axios from 'axios'
import useSWR from 'swr'
import { apiGetCustomer } from '@/services/UserService.js'
import { useAuth } from '@/auth/index.js'

const CustomerEdit = () => {
    const { userId } = useParams()
    const navigate = useNavigate()

    // 상태 변수들 선언
    const [saveDialogOpen, setSaveDialogOpen] = useState(false)
    const [alertDialogOpen, setAlertDialogOpen] = useState(false)
    const [alertMessage, setAlertMessage] = useState('')
    const [alertTitle, setAlertTitle] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [passwordError, setPasswordError] = useState('')
    const [passwordResetDialogOpen, setPasswordResetDialogOpen] = useState(false) // 비밀번호 초기화 다이얼로그
    const [isResettingPassword, setIsResettingPassword] = useState(false) // 비밀번호 초기화 로딩 상태
    const [formData, setFormData] = useState({
        userId: '',
        userName: '',
        userSeCd: '',
        newPassword: '',
        confirmPassword: '',
    })

    const { user } = useAuth();

    const mg = user.userSeCd === 'MG';

    // 권한 옵션 설정
    const roleOptions = [
        { value: 'CU', label: '고객사' },
        { value: 'DM', label: '결함검토/할당(dev manager)' },
        { value: 'DP', label: '결함처리(developer)' },
        { value: 'MG', label: '처리현황 조회(manager)' },
        { value: 'QA', label: '결함등록/완료(Q/A)' },
    ]

    const { data, isLoading, error } = useSWR(
        userId ? ['/users/read', { userId }] : null,
        // eslint-disable-next-line no-unused-vars
        ([_, params]) => apiGetCustomer(params),
        {
            revalidateOnFocus: false,
            revalidateIfStale: false,
            revalidateOnMount: true,
        },
    )

    // 데이터가 로드되면 폼 데이터 설정
    useEffect(() => {
        if (data) {
            setFormData({
                userId: data.userId || '',
                userName: data.userName || '',
                userSeCd: data.userSeCd || '',
                newPassword: '',
                confirmPassword: '',
            })
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
                    사용자 정보를 불러오는 중 오류가 발생했습니다. 다시 시도해
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

        // 비밀번호 일치 여부 확인
        if (
            name === 'confirmPassword' ||
            (name === 'newPassword' && formData.confirmPassword)
        ) {
            if (name === 'newPassword' && value !== formData.confirmPassword) {
                setPasswordError('비밀번호가 일치하지 않습니다.')
            } else if (
                name === 'confirmPassword' &&
                value !== formData.newPassword
            ) {
                setPasswordError('비밀번호가 일치하지 않습니다.')
            } else {
                setPasswordError('')
            }
        }
    }

    const handleSelectChange = (selectedOption) => {
        if (selectedOption) {
            setFormData((prev) => ({
                ...prev,
                userSeCd: selectedOption.value,
            }))
        }
    }

    const handleBackToList = () => {
        navigate('/user-management')
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

    // 저장 다이얼로그 관련 함수 추가
    const handleSaveDialogClose = () => {
        setSaveDialogOpen(false)
    }

    const handleSaveDialogOpen = (e) => {
        e.preventDefault() // 폼 제출 방지

        // 권한 선택 여부 확인
        if (!formData.userSeCd) {
            showAlert('권한 미선택', '권한을 선택해주세요.')
            return
        }

        // 비밀번호 변경 시 새 비밀번호 입력 여부 확인
        if (formData.confirmPassword && !formData.newPassword) {
            showAlert('비밀번호 미입력', '새 비밀번호를 입력해주세요.')
            return
        }

        // 비밀번호 필드가 하나만 입력된 경우 확인
        if (
            (formData.newPassword && !formData.confirmPassword) ||
            (!formData.newPassword && formData.confirmPassword)
        ) {
            showAlert(
                '비밀번호 확인 필요',
                '비밀번호와 비밀번호 확인이 모두 입력되어야 합니다.',
            )
            return
        }

        // 비밀번호 불일치 확인
        if (
            formData.newPassword &&
            formData.confirmPassword &&
            formData.newPassword !== formData.confirmPassword
        ) {
            showAlert('비밀번호 불일치', '비밀번호가 일치하지 않습니다.')
            return
        }

        // 모든 검증을 통과하면 저장 다이얼로그 열기
        setSaveDialogOpen(true)
    }

    // 비밀번호 초기화 다이얼로그 열기
    const handlePasswordResetDialogOpen = () => {
        setPasswordResetDialogOpen(true)
    }

    // 비밀번호 초기화 다이얼로그 닫기
    const handlePasswordResetDialogClose = () => {
        setPasswordResetDialogOpen(false)
    }

    // 비밀번호 초기화 처리
    const handlePasswordReset = async () => {
        try {
            setIsResettingPassword(true)

            // 서버에 비밀번호 초기화 요청
            await axios.post(
                `${apiPrefix}/users/resetPassword`,
                {
                    userId: formData.userId,
                },
                {
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    withCredentials: true,
                },
            )

            toast.push(
                <Notification title={'비밀번호 횟수 초기화 완료'} type="success">
                    비밀번호 횟수가 성공적으로 초기화되었습니다.
                </Notification>,
            )

            // 비밀번호 입력 필드 초기화
            setFormData((prev) => ({
                ...prev,
                newPassword: '',
                confirmPassword: '',
            }))

            setPasswordError('')

        } catch (error) {
            toast.push(
                <Notification title={'비밀번호 초기화 실패'} type="danger">
                    {error.response?.data?.error}
                </Notification>,
            )
        } finally {
            setIsResettingPassword(false)
            setPasswordResetDialogOpen(false)
        }
    }

    // 실제 저장 처리 함수
    const handleSave = async () => {
        try {
            setIsSubmitting(true)

            // 서버에 사용자 정보 업데이트 요청
            await axios.put(
                `${apiPrefix}/users/modifyUser`,
                {
                    userId: formData.userId,
                    userName: formData.userName,
                    userSeCd: formData.userSeCd,
                    password: formData.newPassword,
                },
                {
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    withCredentials: true,
                },
            )

            toast.push(
                <Notification title={'성공적으로 수정됨'} type="success">
                    사용자 정보가 성공적으로 수정되었습니다
                </Notification>,
            )

            // 비밀번호 필드 초기화
            setFormData((prev) => ({
                ...prev,
                newPassword: '',
                confirmPassword: '',
            }))

            setPasswordError('')

        } catch (error) {
            toast.push(
                <Notification title={'수정 실패'} type="danger">
                    {error.response?.data?.error ||
                        '사용자 정보 수정이 실패했습니다.'}
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
                    <h4 className="font-bold">사용자 정보 수정</h4>
                    <div className="flex gap-2">
                        <Button
                            type="button"
                            icon={<HiOutlineArrowLeft />}
                            onClick={handleBackToList}
                        >
                            취소
                        </Button>
                        <Button
                            type="button"
                            variant="twoTone"
                            color="orange"
                            icon={<TbRefresh />}
                            onClick={handlePasswordResetDialogOpen}
                            loading={isResettingPassword}
                        >
                            비밀번호 초기화
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

                <div className="flex flex-col xl:justify-between h-full 2xl:min-w-[360px] mx-auto">
                    <div className="flex xl:flex-col items-center gap-4 mt-6">
                        <Avatar size={90} shape="circle" icon={<TbUser />} />
                        <h4 className="font-bold">{formData.userName}</h4>
                    </div>

                    <div className="grid grid-cols-1 gap-y-7 mt-10">
                        <div>
                            <label className="font-semibold block mb-2">
                                사용자 ID
                            </label>
                            <Input
                                type="text"
                                name="userId"
                                value={formData.userId}
                                onChange={handleInputChange}
                                disabled // ID는 수정 불가
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                사용자명
                            </label>
                            <Input
                                type="text"
                                name="userName"
                                value={formData.userName}
                                onChange={handleInputChange}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                권한
                            </label>
                            <Select
                                options={roleOptions}
                                isSearchable={false}
                                isDisabled={!mg}
                                value={
                                    roleOptions.find(
                                        (option) =>
                                            option.value === formData.userSeCd,
                                    ) || null
                                }
                                onChange={handleSelectChange}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                새 비밀번호
                            </label>
                            <Input
                                type="password"
                                name="newPassword"
                                value={formData.newPassword}
                                onChange={handleInputChange}
                                placeholder="변경하려면 새 비밀번호 입력"
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">
                                비밀번호 확인
                            </label>
                            <Input
                                type="password"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleInputChange}
                                placeholder="비밀번호 확인"
                            />
                            {passwordError && (
                                <div className="text-red-500 text-sm mt-1">
                                    {passwordError}
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                {/* 저장 확인 다이얼로그 */}
                <ConfirmDialog
                    isOpen={saveDialogOpen}
                    title="사용자 정보 수정"
                    onClose={handleSaveDialogClose}
                    onRequestClose={handleSaveDialogClose}
                    onCancel={handleSaveDialogClose}
                    onConfirm={handleSave}
                    confirmText={'수정'}
                >
                    <p>사용자 정보를 수정하시겠습니까?</p>
                </ConfirmDialog>

                {/* 비밀번호 초기화 확인 다이얼로그 */}
                <ConfirmDialog
                    isOpen={passwordResetDialogOpen}
                    title="비밀번호 초기화"
                    onClose={handlePasswordResetDialogClose}
                    onRequestClose={handlePasswordResetDialogClose}
                    onCancel={handlePasswordResetDialogClose}
                    onConfirm={handlePasswordReset}
                    confirmText={'초기화'}
                    confirmButtonProps={{ color: 'red-600' }}
                >
                    <p>
                        비밀번호를 초기화하시겠습니까?
                    </p>
                    <p className="text-sm text-gray-600 mt-2">
                        초기화 후 임시 비밀번호가 발급됩니다.
                    </p>
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

export default CustomerEdit