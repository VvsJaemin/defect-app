import { useEffect, useState } from 'react'
import appConfig from '@/configs/app.config'
import { REDIRECT_URL_KEY } from '@/constants/app.constant'
import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '@/auth'

const { unAuthenticatedEntryPath } = appConfig

const ProtectedRoute = () => {
    const { authenticated } = useAuth()
    const location = useLocation()
    const [shouldRedirect, setShouldRedirect] = useState(false)
    const [redirectUrl, setRedirectUrl] = useState('')

    useEffect(() => {
        // 인증되지 않은 사용자가 protected 경로에 접근하는 경우
        if (!authenticated) {
            const pathName = location.pathname
            const getPathName = pathName === '/' ? '' : `?${REDIRECT_URL_KEY}=${encodeURIComponent(pathName)}`

            // 약간의 지연을 두어 무한 루프 방지
            const timer = setTimeout(() => {
                setRedirectUrl(`${unAuthenticatedEntryPath}${getPathName}`)
                setShouldRedirect(true)
            }, 100)

            return () => clearTimeout(timer)
        } else {
            setShouldRedirect(false)
        }
    }, [authenticated, location.pathname])

    // 인증되지 않은 사용자를 로그인 페이지로 리디렉션
    if (!authenticated && shouldRedirect) {
        return <Navigate replace to={redirectUrl} />
    }

    // 인증된 사용자는 정상적으로 컴포넌트 렌더링
    return <Outlet />
}

export default ProtectedRoute