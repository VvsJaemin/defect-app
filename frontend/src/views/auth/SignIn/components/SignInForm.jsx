import { useState, useEffect } from 'react';
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
        .string({ required_error: '아이디를 입력하세요.' })
        .min(1, { message: '아이디를 입력하세요.' }),
    password: z
        .string({ required_error: '비밀번호를 입력하세요.' })
        .min(1, { message: '비밀번호를 입력하세요.' }),
});

const SignInForm = (props) => {
    const [isSubmitting, setSubmitting] = useState(false);
    const [rememberUserId, setRememberUserId] = useState(false);

    const { disableSubmit = false, className, setMessage, passwordHint } = props;

    const {
        handleSubmit,
        formState: { errors },
        control,
        setValue,
    } = useForm({
        resolver: zodResolver(validationSchema),
    });

    const { signIn } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    // 컴포넌트 마운트 시 저장된 아이디 불러오기
    useEffect(() => {
        const savedUserId = localStorage.getItem('rememberedUserId');
        if (savedUserId) {
            setValue('userId', savedUserId);
            setRememberUserId(true);
        }
    }, [setValue]);

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
                    // 로그인 성공 시 아이디 저장 처리
                    if (rememberUserId) {
                        localStorage.setItem('rememberedUserId', userId);
                    } else {
                        localStorage.removeItem('rememberedUserId');
                    }
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
                    invalid={Boolean(errors.userId)}
                    errorMessage={errors.userId?.message}
                >
                    <Controller
                        name="userId"
                        control={control}
                        render={({ field }) => (
                            <Input
                                placeholder="아이디"
                                autoComplete="off"
                                {...field}
                            />
                        )}
                    />
                </FormItem>
                <FormItem
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
                                placeholder="비밀번호"
                                autoComplete="off"
                                {...field}
                            />
                        )}
                    />
                </FormItem>
                {passwordHint}

                {/* 아이디 기억하기 체크박스 */}
                <div className="flex items-center mb-6">
                    <input
                        type="checkbox"
                        id="rememberUserId"
                        checked={rememberUserId}
                        onChange={(e) => setRememberUserId(e.target.checked)}
                        className="mr-2 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                    />
                    <label htmlFor="rememberUserId" className="text-sm text-gray-500 cursor-pointer">
                        아이디 기억하기
                    </label>
                </div>


                <Button
                    block
                    loading={isSubmitting}
                    variant="solid"
                    type="submit"
                    disabled={disableSubmit}
                >
                    {isSubmitting ? '로그인 중' : '로그인'}
                </Button>
            </Form>
        </div>
    );
};

export default SignInForm;