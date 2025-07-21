import { useEffect, useState, useMemo } from 'react'
import Card from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Avatar from '@/components/ui/Avatar/Avatar'
import Notification from '@/components/ui/Notification'
import toast from '@/components/ui/toast'
import ConfirmDialog from '@/components/shared/ConfirmDialog'
import Input from '@/components/ui/Input'
import Select from '@/components/ui/Select'
import { HiOutlineArrowLeft, HiSave } from 'react-icons/hi'
import { TbRefresh, TbUser } from 'react-icons/tb'
import { useNavigate, useParams } from 'react-router'
import ApiService from '@/services/ApiService' // axios 대신 ApiService 사용
import useSWR from 'swr'
import { useAuth } from '@/auth/index.js'
import { apiGetUser } from '@/services/UserService.js'

const UserEdit = () => {
    const { userId: rawUserId } = useParams()
    const navigate = useNavigate()

    // userId URL 디코딩 처리
    const userId = useMemo(() => {
        if (!rawUserId) return null

        try {
            // URL 디코딩 시도
            let decoded = rawUserId

            // 다중 인코딩 체크 및 디코딩
            while (decoded.includes('%')) {
                const newDecoded = decodeURIComponent(decoded)
                if (newDecoded === decoded) break // 더 이상 디코딩되지 않으면 중단
                decoded = newDecoded
            }

            console.log('원본 userId:', rawUserId)
            console.log('디코딩된 userId:', decoded)
            return decoded
        } catch (error) {
            console.error('userId 디코딩 실패:', error)
            // 디코딩 실패시 원본 값 사용
            return rawUserId
        }
    }, [rawUserId])

    // 상태 변수들 선언
    const [saveDialogOpen, setSaveDialogOpen] = useState(false)
    const [alertDialogOpen, setAlertDialogOpen] = useState(false)
    const [alertMessage, setAlertMessage] = useState('')
    const [alertTitle, setAlertTitle] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [passwordError, setPasswordError] = useState('')
    const [passwordResetDialogOpen, setPasswordResetDialogOpen] = useState(false)
    const [isResettingPassword, setIsResettingPassword] = useState(false)
    const [formData, setFormData] = useState({
        userId: '',
        userName: '',
        userSeCd: '',
        newPassword: '',
        confirmPassword: '',
    })

    const { user, signOut } = useAuth()

    // 디버깅 로그 추가
    useEffect(() => {
        console.log('=== UserEdit 디버깅 정보 ===')
        console.log('현재 URL:', window.location.href)
        console.log('useParams() 원본:', rawUserId)
        console.log('디코딩된 userId:', userId)
        console.log('User Agent:', navigator.userAgent)
        console.log('모바일 여부:', /Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent))
        console.log('============================')
    }, [rawUserId, userId])

    // 초기 로딩 상태 확인
    if (!user) {
        return (
            <div className="w-full p-5">
                <div className="text-center">인증 정보를 확인하는 중입니다...</div>
            </div>
        )
    }

    if (!userId) {
        return (
            <div className="w-full p-5">
                <div className="text-center text-red-500">
                    사용자 ID가 누락되었습니다.
                    <br />
                    <small className="text-gray-500">
                        원본: {rawUserId}
                    </small>
                    <br />
                    <small className="text-gray-500">
                        디코딩됨: {userId}
                    </small>
                </div>
            </div>
        )
    }

    const mg = user.userSeCd === 'MG'

    // 현재 로그인한 사용자의 본인 계정인지 확인 (디코딩된 userId와 비교)
    const isOwnAccount = user.userId === userId

    // 권한 옵션 설정
    const roleOptions = [
        { value: 'CU', label: '고객사' },
        { value: 'DM', label: '결함검토/할당(dev manager)' },
        { value: 'DP', label: '결함처리(developer)' },
        { value: 'MG', label: '처리현황 조회(manager)' },
        { value: 'QA', label: '결함등록/완료(Q/A)' },
    ]

    // SWR을 사용하여 사용자 정보 조회 (디코딩된 userId 사용)
    const { data, isLoading, error, mutate } = useSWR(
        userId ? `user-${userId}` : null,
        async () => {
            console.log('API 호출 시작 - userId:', userId)

            try {
                const result = await apiGetUser({ userId: userId })
                console.log('API 응답:', result)
                return result
            } catch (err) {
                console.error('API 오류:', err)
                throw err
            }
        },
        {
            revalidateOnFocus: false,
            revalidateIfStale: false,
            revalidateOnMount: true,
        }
    )

    // 데이터가 로드되면 폼 데이터 설정
    useEffect(() => {
        console.log('데이터 변경:', data)
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

    // userId가 변경되면 데이터 다시 로드
    useEffect(() => {
        console.log('userId 변경됨:', userId)
        if (userId) {
            mutate()
        }
    }, [userId, mutate])

    if (isLoading) {
        return (
            <div className="w-full p-5">
                <div className="text-center">
                    데이터를 불러오는 중입니다...
                    <br />
                    <small className="text-gray-500 mt-2 block">
                        요청 중인 userId: {userId}
                    </small>
                </div>
            </div>
        )
    }

    if (error) {
        console.error('UserEdit 에러:', error)
        return (
            <div className="w-full p-5">
                <div className="text-center text-red-500">
                    사용자 정보를 불러오는 중 오류가 발생했습니다. 다시 시도해 주세요.
                    <br />
                    <small className="text-gray-500 mt-2 block">
                        에러: {error.message || '알 수 없는 오류'}
                    </small>
                    <small className="text-gray-500 block">
                        원본 userId: {rawUserId}
                    </small>
                    <small className="text-gray-500 block">
                        디코딩된 userId: {userId}
                    </small>
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
        e.preventDefault()

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

            // 디코딩된 userId 사용
            await ApiService.post('/users/resetPassword', {
                userId: userId,
            })

            toast.push(
                <Notification
                    title={'비밀번호 횟수 초기화 완료'}
                    type="success"
                >
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

            // 본인 계정의 비밀번호 초기화인 경우 로그아웃 처리
            if (isOwnAccount) {
                setTimeout(() => {
                    signOut()
                    navigate('/sign-in')
                }, 2000)
            }
        } catch (error) {
            toast.push(
                <Notification title={'비밀번호 초기화 실패'} type="danger">
                    {error.response?.data?.error ||
                        '비밀번호 초기화에 실패했습니다.'}
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

            // 본인 계정의 비밀번호 변경인지 확인
            const isPasswordChanged = isOwnAccount && formData.newPassword

            // 디코딩된 userId 사용
            await ApiService.put('/users/modifyUser', {
                userId: userId, // 디코딩된 userId 사용
                userName: formData.userName,
                userSeCd: formData.userSeCd,
                password: formData.newPassword,
            })

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

            // 본인 계정의 비밀번호가 변경된 경우 로그아웃 처리
            if (isPasswordChanged) {
                setTimeout(() => {
                    signOut()
                    navigate('/sign-in')
                }, 2000)
            }
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
                        <h4 className="font-bold">
                            {formData.userName}
                        </h4>
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
                                disabled
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
                    {isOwnAccount && formData.newPassword && (
                        <p className="text-sm text-yellow-600 mt-2">
                            ⚠️ 비밀번호 변경으로 인해 잠시 후 로그아웃됩니다.
                        </p>
                    )}
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
                    <p>비밀번호를 초기화하시겠습니까?</p>
                    <p className="text-sm text-gray-600 mt-2">
                        초기화 후 임시 비밀번호가 발급됩니다.
                    </p>
                    {isOwnAccount && (
                        <p className="text-sm text-yellow-600 mt-2">
                            ⚠️ 본인 계정의 비밀번호 초기화로 인해 로그아웃됩니다.
                        </p>
                    )}
                </ConfirmDialog>

                {/* 경고 다이얼로그 */}
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

export default UserEdit