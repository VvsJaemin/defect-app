import { useNavigate } from 'react-router-dom'
import Container from '@/components/shared/Container'
import SpaceSignBoard from '@/assets/svg/SpaceSignBoard'

const AccessDenied = () => {
    const navigate = useNavigate()

    const handleGoBack = () => {
        try {
            navigate(-1)
        } catch (error) {
            console.error('Navigate back failed:', error)
            // 실패하면 홈으로 이동
            navigate('/')
        }

    }


    return (
        <Container className="h-full">
            <div className="h-full flex flex-col items-center justify-center">
                <SpaceSignBoard height={280} width={280} />
                <div className="mt-10 text-center">
                    <h3 className="mb-2">Access Denied!</h3>
                    <p className="text-base mb-6">
                        이 페이지에 접근할 권한이 없습니다
                    </p>
                    <button
                        onClick={handleGoBack}
                        className="px-6 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors duration-200"
                    >
                        이전 페이지로
                    </button>
                </div>
            </div>
        </Container>
    )
}

export default AccessDenied