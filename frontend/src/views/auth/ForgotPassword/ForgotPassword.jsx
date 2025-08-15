import { useState, useEffect } from 'react'
import Alert from '@/components/ui/Alert'
import Button from '@/components/ui/Button'
import ActionLink from '@/components/shared/ActionLink'
import ForgotPasswordForm from './components/ForgotPasswordForm'
import useTimeOutMessage from '@/utils/hooks/useTimeOutMessage'
import { useNavigate } from 'react-router'

export const ForgotPasswordBase = ({ signInUrl = '/sign-in' }) => {
    const [emailSent, setEmailSent] = useState(false)
    const [message, setMessage] = useTimeOutMessage()
    const [isSuccess, setIsSuccess] = useState(false)
    const [countdown, setCountdown] = useState(3)

    const navigate = useNavigate()

    const handleContinue = () => {
        navigate(signInUrl)
    }

    // 이메일 전송 성공 시 카운트다운 시작
    useEffect(() => {
        let timer
        if (emailSent && isSuccess) {
            timer = setInterval(() => {
                setCountdown((prev) => {
                    if (prev <= 1) {
                        clearInterval(timer)
                        return 0
                    }
                    return prev - 1
                })
            }, 1000)
        }

        return () => {
            if (timer) clearInterval(timer)
        }
    }, [emailSent, isSuccess])

    return (
        <div>
            <div className="mb-6">
                {emailSent ? (
                    <>
                        <h3 className="mb-2">이메일을 확인하세요</h3>
                        <p className="font-semibold heading-text">
                            임시 비밀번호를 이메일로 전송했습니다.
                            이메일을 확인하신 후 로그인해주세요.
                        </p>
                        {isSuccess && countdown > 0 && (
                            <p className="text-sm text-gray-500 mt-2">
                                {countdown}초 후 로그인 페이지로 자동 이동됩니다.
                            </p>
                        )}
                    </>
                ) : (
                    <>
                        <h3 className="mb-2">비밀번호 찾기</h3>
                        <p className="font-semibold heading-text">
                            사용 중인 아이디를 입력하시면 임시 비밀번호를 전송해드립니다.
                        </p>
                    </>
                )}
            </div>
            {message && (
                <Alert showIcon className="mb-4" type={isSuccess ? "success" : "danger"}>
                    <span className="break-all">{message}</span>
                </Alert>
            )}
            <ForgotPasswordForm
                emailSent={emailSent}
                setMessage={setMessage}
                setEmailSent={setEmailSent}
                setIsSuccess={setIsSuccess}
            >
                {/*<Button*/}
                {/*    block*/}
                {/*    variant="solid"*/}
                {/*    type="button"*/}
                {/*    onClick={handleContinue}*/}
                {/*>*/}
                {/*    로그인 페이지로 이동*/}
                {/*</Button>*/}
            </ForgotPasswordForm>
            <div className="mt-4 text-center">
                <span>로그인 페이지로 </span>
                <ActionLink
                    to={signInUrl}
                    className="heading-text font-bold"
                    themeColor={false}
                >
                    돌아가기
                </ActionLink>
            </div>
        </div>
    )
}

const ForgotPassword = () => {
    return <ForgotPasswordBase />
}

export default ForgotPassword