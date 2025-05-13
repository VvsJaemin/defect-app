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
import { useNavigate, useParams } from 'react-router'
import { TbUser } from 'react-icons/tb'
import { apiPrefix } from '@/configs/endpoint.config.js'
import axios from 'axios'
import useSWR from 'swr'
import { apiGetCustomer } from '@/services/UserService.js'

// CustomerEdit.js 부분 수정

// 필요한 import는 유지

const CustomerEdit = () => {
    const { userId } = useParams()
    const navigate = useNavigate()

    // 상태 변수들 선언
    const [dialogOpen, setDialogOpen] = useState(false)
    const [saveDialogOpen, setSaveDialogOpen] = useState(false) // 저장 확인 다이얼로그용 상태 추가
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [passwordError, setPasswordError] = useState('')
    const [formData, setFormData] = useState({
        userId: '',
        userName: '',
        userSeCd: '',
        newPassword: '',
        confirmPassword: '',
    })

    // 권한 옵션 설정
    const roleOptions = [
        { value: 'ADMIN', label: '관리자' },
        { value: 'USER', label: '일반 사용자' },
        { value: 'GUEST', label: '게스트' },
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

    // 저장 다이얼로그 관련 함수 추가
    const handleSaveDialogClose = () => {
        setSaveDialogOpen(false)
    }

    const handleSaveDialogOpen = (e) => {
        e.preventDefault() // 폼 제출 방지
        setSaveDialogOpen(true)
    }

    const handlePasswordChange = async () => {
        // 비밀번호 필드가 모두 비어있으면 비밀번호 변경을 시도하지 않음
        if (!formData.newPassword && !formData.confirmPassword) {
            return true
        }

        // 비밀번호 유효성 검사
        if (!formData.newPassword) {
            setPasswordError('새 비밀번호를 입력해주세요.')
            return false
        }

        if (formData.newPassword !== formData.confirmPassword) {
            setPasswordError('비밀번호가 일치하지 않습니다.')
            return false
        }

        try {
            // 서버에 비밀번호 변경 요청
            await axios.put(
                `${apiPrefix}/users/change-password/${userId}`,
                {
                    newPassword: formData.newPassword,
                },
                {
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    withCredentials: true,
                },
            )

            toast.push(
                <Notification title={'비밀번호 변경 성공'} type="success">
                    비밀번호가 성공적으로 변경되었습니다
                </Notification>,
            )

            // 비밀번호 필드 초기화
            setFormData((prev) => ({
                ...prev,
                newPassword: '',
                confirmPassword: '',
            }))

            return true
        } catch (error) {
            toast.push(
                <Notification title={'비밀번호 변경 실패'} type="danger">
                    {error.response?.data?.error ||
                        '비밀번호 변경에 실패했습니다.'}
                </Notification>,
            )
            return false
        }
    }

    // 실제 저장 처리 함수
    const handleSave = async () => {
        try {
            setIsSubmitting(true)

            // 비밀번호 변경이 필요하다면 처리
            if (formData.newPassword || formData.confirmPassword) {
                const passwordChangeSuccess = await handlePasswordChange()
                if (!passwordChangeSuccess) {
                    setIsSubmitting(false)
                    return
                }
            }

            // 서버에 사용자 정보 업데이트 요청
            await axios.put(
                `${apiPrefix}/users/update/${userId}`,
                {
                    userId: formData.userId,
                    userName: formData.userName,
                    userSeCd: formData.userSeCd,
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

            // 상세 페이지로 이동
            navigate(`/user-management/details/${userId}`)
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

    // 현재 선택된 권한 옵션 계산
    const selectedRole =
        roleOptions.find((option) => option.value === formData.userSeCd) || null

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
                                value={selectedRole}
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
                    title="사용자 정보 저장"
                    onClose={handleSaveDialogClose}
                    onRequestClose={handleSaveDialogClose}
                    onCancel={handleSaveDialogClose}
                    onConfirm={handleSave}
                    confirmText={'저장'}
                >
                    <p>사용자 정보를 저장하시겠습니까?</p>
                </ConfirmDialog>
            </form>
        </Card>
    )
}

export default CustomerEdit