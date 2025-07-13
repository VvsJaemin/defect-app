
import { Navigate, Outlet, useLocation } from 'react-router'
import appConfig from '@/configs/app.config'
import { useAuth } from '@/auth'
import { useEffect, useState } from 'react'

const { authenticatedEntryPath } = appConfig

const PublicRoute = () => {
    const { authenticated } = useAuth()
    const location = useLocation()
    const [shouldRedirect, setShouldRedirect] = useState(false)

    useEffect(() => {
        // 인증된 사용자가 public 경로에 접근하는 경우에만 리디렉션
        if (authenticated) {
            // 약간의 지연을 두어 무한 루프 방지
            const timer = setTimeout(() => {
                setShouldRedirect(true)
            }, 100)

            return () => clearTimeout(timer)
        } else {
            setShouldRedirect(false)
        }
    }, [authenticated])

    // 인증된 사용자가 로그인 페이지에 접근하는 경우 리디렉션
    if (authenticated && shouldRedirect) {
        return <Navigate replace to={authenticatedEntryPath} />
    }

    return <Outlet />
}

export default PublicRoute