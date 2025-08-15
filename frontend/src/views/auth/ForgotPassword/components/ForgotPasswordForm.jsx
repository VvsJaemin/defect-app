import { useState } from 'react'
import Input from '@/components/ui/Input'
import Button from '@/components/ui/Button'
import { FormItem, Form } from '@/components/ui/Form'
import { apiForgotPassword } from '@/services/AuthService'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'

const validationSchema = z.object({
    email: z.string().email('올바른 이메일 형식을 입력하세요').min(1, '이메일은 필수입니다'),
})

const ForgotPasswordForm = (props) => {
    const [isSubmitting, setSubmitting] = useState(false)

    const { className, setMessage, setEmailSent, emailSent, children } = props

    const {
        handleSubmit,
        formState: { errors },
        control,
    } = useForm({
        resolver: zodResolver(validationSchema),
        defaultValues: {
            email: ''
        }
    })

    const onForgotPassword = async (values) => {
        const { email } = values
        setSubmitting(true)

        try {
            const resp = await apiForgotPassword({ email })
            if (resp && resp.result === 'success') {
                setEmailSent?.(true)
                setMessage?.('')
            }
        } catch (error) {
            console.error('비밀번호 찾기 오류:', error)
            let errorMessage = '비밀번호 찾기 중 오류가 발생했습니다.'

            if (error.response?.data?.message) {
                errorMessage = error.response.data.message
            } else if (typeof error === 'string') {
                errorMessage = error
            }

            setMessage?.(errorMessage)
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className={className}>
            {!emailSent ? (
                <Form onSubmit={handleSubmit(onForgotPassword)}>
                    <FormItem
                        label="이메일"
                        invalid={Boolean(errors.email)}
                        errorMessage={errors.email?.message}
                    >
                        <Controller
                            name="email"
                            control={control}
                            render={({ field }) => (
                                <Input
                                    type="email"
                                    placeholder="이메일을 입력하세요"
                                    autoComplete="email"
                                    {...field}
                                />
                            )}
                        />
                    </FormItem>
                    <Button
                        block
                        loading={isSubmitting}
                        variant="solid"
                        type="submit"
                        disabled={isSubmitting}
                    >
                        {isSubmitting ? '전송 중...' : '임시 비밀번호 전송'}
                    </Button>
                </Form>
            ) : (
                <>{children}</>
            )}
        </div>
    )
}

export default ForgotPasswordForm