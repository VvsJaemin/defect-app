import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import { FormItem, Form } from '@/components/ui/Form';
import PasswordInput from '@/components/shared/PasswordInput';
import classNames from '@/utils/classNames';
import { useAuth } from '@/auth';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const validationSchema = z.object({
    userId: z
        .string({ required_error: '계정을 입력하세요.' })
        .min(1, { message: '계정을 입력하세요.' }),
    password: z
        .string({ required_error: '비밀번호를 입력하세요.' })
        .min(1, { message: '비밀번호를 입력하세요.' }),
});

const SignInForm = (props) => {
    const [isSubmitting, setSubmitting] = useState(false);

    const { disableSubmit = false, className, setMessage, passwordHint } = props;

    const {
        handleSubmit,
        formState: { errors },
        control,
    } = useForm({
        // defaultValues: {
        //     email: 'admin-01@ecme.com',
        //     password: '123Qwe',
        // },
        resolver: zodResolver(validationSchema),
    });

    const { signIn } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    // 쿼리 파라미터에서 redirectUrl 가져오기
    const queryParams = new URLSearchParams(location.search);
    let redirectUrl = queryParams.get('redirectUrl') || '/home';

    // redirectUrl 유효성 검증
    if (!redirectUrl.startsWith('/')) {
        console.warn('유효하지 않은 redirectUrl:', redirectUrl);
        redirectUrl = '/home';
    }

    const onSignIn = async (values) => {
        const { userId, password } = values;

        if (!disableSubmit) {
            setSubmitting(true);

            try {
                const result = await signIn({ userId, password });

                if (result?.status === 'failed') {
                    setMessage?.(result.message);
                } else {
                    navigate(redirectUrl, { replace: true });
                }
            } catch (error) {
                console.error('로그인 오류:', error);
                setMessage?.('로그인 중 오류가 발생했습니다.');
            } finally {
                setSubmitting(false);
            }
        }
    };

    return (
        <div className={className}>
            <Form onSubmit={handleSubmit(onSignIn)}>
                <FormItem
                    label="Email"
                    invalid={Boolean(errors.userId)}
                    errorMessage={errors.userId?.message}
                >
                    <Controller
                        name="userId"
                        control={control}
                        render={({ field }) => (
                            <Input
                                placeholder="Email"
                                autoComplete="off"
                                {...field}
                            />
                        )}
                    />
                </FormItem>
                <FormItem
                    label="Password"
                    invalid={Boolean(errors.password)}
                    errorMessage={errors.password?.message}
                    className={classNames(
                        passwordHint ? 'mb-0' : '',
                        errors.password?.message ? 'mb-8' : '',
                    )}
                >
                    <Controller
                        name="password"
                        control={control}
                        rules={{ required: true }}
                        render={({ field }) => (
                            <PasswordInput
                                type="password"
                                placeholder="Password"
                                autoComplete="off"
                                {...field}
                            />
                        )}
                    />
                </FormItem>
                {passwordHint}
                <Button
                    block
                    loading={isSubmitting}
                    variant="solid"
                    type="submit"
                    disabled={disableSubmit}
                >
                    {isSubmitting ? 'Signing in...' : 'Sign In'}
                </Button>
            </Form>
        </div>
    );
};

export default SignInForm;