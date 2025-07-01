import { useNavigate } from 'react-router-dom'
import Container from '@/components/shared/Container'
import SpaceSignBoard from '@/assets/svg/SpaceSignBoard'

const NotFound = () => {
    const navigate = useNavigate()

    const handleGoBack = () => {
        window.history.back()
    }

    const handleGoHome = () => {
        navigate('/')
    }

    return (
        <Container className="h-full">
            <div className="h-full flex flex-col items-center justify-center">
                <SpaceSignBoard height={280} width={280} />
                <div className="mt-10 text-center">
                    <h3 className="mb-2">404 - Page Not Found!</h3>
                    <p className="text-base mb-6">
                        요청하신 페이지를 찾을 수 없습니다
                    </p>
                    <div className="flex gap-4 justify-center">
                        <button
                            onClick={handleGoBack}
                            className="px-6 py-2 bg-gray-500 hover:bg-gray-600 text-white rounded-lg transition-colors duration-200"
                        >
                            이전 페이지로
                        </button>
                        <button
                            onClick={handleGoHome}
                            className="px-6 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors duration-200"
                        >
                            홈으로 이동
                        </button>
                    </div>
                </div>
            </div>
        </Container>
    )
}

export default NotFound