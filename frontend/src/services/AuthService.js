import ApiService from './ApiService'
import { tokenManager } from '@/utils/hooks/tokenManager.jsx'
import { cookieHelpers } from '@/utils/cookiesStorage.js'
import { userIdStorage } from '@/utils/userIdStorage.js'

// 토큰 쿠키 설정 대기 함수
const waitForTokenCookie = (maxAttempts = 30) => {
    return new Promise((resolve) => {
        let attempts = 0
        const checkCookie = () => {
            const token = cookieHelpers.getAccessToken()
            if (token || attempts >= maxAttempts) {
                resolve(token)
            } else {
                attempts++
                setTimeout(checkCookie, 100)
            }
        }
        checkCookie()
    })
}

export async function apiSignIn(data) {
    try {
        const response = await ApiService.post('/auth/sign-in', data)

        if (response.data.result === 'success') {
            // 쿠키 설정 완료까지 대기
            const accessToken = await waitForTokenCookie()
            const userInfo = cookieHelpers.getUserInfo()

            const userDatas = response.data.userData // 또는 실제 사용자 데이터 구조에 맞게 수정

            // 로컬스토리지에 userId 저장
            if (userDatas && userDatas.userId) {
                userIdStorage.setUserId(userDatas.userId)
            }



            // 토큰 매니저에 토큰 설정
            if (accessToken) {
                tokenManager.setAccessToken(accessToken)

                // 토큰 만료 시간 설정
                const expiryTime = cookieHelpers.getTokenExpiry()
                if (expiryTime) {
                    tokenManager.setTokenExpiry(expiryTime)
                }
            }

            // 사용자 정보 반환
            const userData = {
                accessToken: accessToken,
                userId: userInfo?.userId || response.data.userId,
                userName: userInfo?.userName || response.data.userName,
                userSeCd: userInfo?.userSeCd || response.data.userSeCd,
                authorities: userInfo?.authorities || response.data.authorities || []
            }

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