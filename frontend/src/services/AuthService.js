import ApiService from './ApiService'
import { tokenManager } from '@/utils/hooks/tokenManager.jsx'

// 쿠키에서 값 가져오기 (httpOnly 포함)
const getCookieValue = (name) => {
    const cookies = document.cookie.split(';')
    for (let cookie of cookies) {
        const [cookieName, cookieValue] = cookie.trim().split('=')
        if (cookieName === name) {
            return cookieValue
        }
    }
    return null
}

// 토큰 쿠키 설정 대기 함수
const waitForTokenCookie = (maxAttempts = 30) => {
    return new Promise((resolve) => {
        let attempts = 0
        const checkCookie = () => {
            const token = getCookieValue('clientAccessToken') || getCookieValue('accessToken')
            if (token || attempts >= maxAttempts) {
                resolve(token)
            } else {
                attempts++
                setTimeout(checkCookie, 100) // 100ms마다 확인
            }
        }
        checkCookie()
    })
}

export async function apiSignIn(data) {
    try {
        const response = await ApiService.post('/auth/sign-in', data)

        if (response.data.result === 'success') {
            console.log('로그인 API 성공:', response.data)

            // 쿠키 설정 완료까지 대기
            const accessToken = await waitForTokenCookie()
            const userInfoStr = getCookieValue('userInfo')

            console.log('쿠키에서 가져온 토큰:', accessToken ? '존재' : '없음')
            console.log('쿠키에서 가져온 사용자 정보:', userInfoStr ? '존재' : '없음')

            let userInfo = null
            if (userInfoStr) {
                try {
                    userInfo = JSON.parse(decodeURIComponent(userInfoStr))
                    console.log('파싱된 사용자 정보:', userInfo)
                } catch (error) {
                    console.error('사용자 정보 파싱 오류:', error)
                }
            }

            // 토큰 매니저에 토큰 설정
            if (accessToken) {
                tokenManager.setAccessToken(accessToken)
            }

            // 사용자 정보 반환 (authStore에서 사용)
            const userData = {
                accessToken: accessToken,
                userId: userInfo?.userId || response.data.userId,
                userName: userInfo?.userName || response.data.userName,
                userSeCd: userInfo?.userSeCd || response.data.userSeCd,
                authorities: userInfo?.authorities || response.data.authorities || []
            }

            console.log('최종 사용자 데이터:', userData)

            // 전역 이벤트로 로그인 성공 알림
            window.dispatchEvent(new CustomEvent('loginSuccess', { detail: userData }))

            return response.data
        }

        throw new Error(response.data.message || '로그인 실패')
    } catch (error) {
        console.error('로그인 오류:', error)
        throw error
    }
}

export async function apiSignOut() {
    try {
        const response = await ApiService.post('/auth/logout')
        tokenManager.removeTokens()
        return response.data
    } catch (error) {
        console.error('로그아웃 오류:', error)
        tokenManager.removeTokens()
        throw error
    }
}

export async function apiSignUp(data) {
    try {
        const response = await ApiService.post('/auth/sign-up', data)
        return response.data
    } catch (error) {
        console.error('회원가입 오류:', error)
        throw error
    }
}