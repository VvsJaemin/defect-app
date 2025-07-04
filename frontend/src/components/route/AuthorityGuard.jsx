import { Navigate, useLocation } from 'react-router'
import useAuthority from '@/utils/hooks/useAuthority'

const AuthorityGuard = (props) => {
    const { userAuthority = [], authority = [], children } = props
    const location = useLocation()

    // userAuthority가 배열이 아닌 경우 배열로 변환
    const userAuthorityArray = Array.isArray(userAuthority)
        ? userAuthority
        : [userAuthority]

    // authority가 배열이 아닌 경우 배열로 변환
    const authorityArray = Array.isArray(authority)
        ? authority
        : [authority]

    const roleMatched = useAuthority(userAuthorityArray, authorityArray)

    return (
        <>
            {roleMatched ? (
                children
            ) : (
                <Navigate
                    to="/access-denied"
                    state={{ from: location.pathname }}
                    replace
                />
            )}
        </>
    )
}

export default AuthorityGuard