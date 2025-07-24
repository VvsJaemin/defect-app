
// 쿠키 저장소 유틸리티
const cookiesStorage = {
    // 쿠키 설정
    setItem: (name, value, expires = 1) => {
        const date = new Date()
        date.setTime(date.getTime() + (expires * 24 * 60 * 60 * 1000))
        const expiresStr = expires ? `; expires=${date.toUTCString()}` : ''
        document.cookie = `${name}=${value}${expiresStr}; path=/`
    },

    // 쿠키 가져오기
    getItem: (name) => {
        const nameEQ = name + '='
        const ca = document.cookie.split(';')
        for (let i = 0; i < ca.length; i++) {
            let c = ca[i]
            while (c.charAt(0) === ' ') c = c.substring(1, c.length)
            if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length)
        }
        return null
    },

    // 쿠키 제거
    removeItem: (name) => {
        document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/`
    },

    // 모든 쿠키 제거
    clear: () => {
        const cookies = document.cookie.split(';')
        cookies.forEach(cookie => {
            const eqPos = cookie.indexOf('=')
            const name = eqPos > -1 ? cookie.substr(0, eqPos) : cookie
            document.cookie = `${name.trim()}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/`
        })
    }
}

// 쿠키 헬퍼 함수들
export const cookieHelpers = {
    // 사용자 정보 쿠키 설정
    setUserInfo: (userInfo, expires = 1) => {
        const userInfoStr = JSON.stringify(userInfo)
        const encodedUserInfo = encodeURIComponent(userInfoStr)
        cookiesStorage.setItem('userInfo', encodedUserInfo, expires)
    },

    // 사용자 정보 쿠키 가져오기
    getUserInfo: () => {
        const userInfoStr = cookiesStorage.getItem('userInfo')
        if (userInfoStr) {
            try {
                return JSON.parse(decodeURIComponent(userInfoStr))
            } catch (error) {
                console.error('사용자 정보 파싱 오류:', error)
                return null
            }
        }
        return null
    },

    // 액세스 토큰 설정
    setAccessToken: (token, expires = 1) => {
        cookiesStorage.setItem('accessToken', token, expires)
    },

    // 액세스 토큰 가져오기
    getAccessToken: () => {
        return cookiesStorage.getItem('accessToken')
    },

    // 토큰 만료 시간 설정
    setTokenExpiry: (expiryTime, expires = 1) => {
        cookiesStorage.setItem('tokenExpiry', expiryTime.toString(), expires)
    },

    // 토큰 만료 시간 가져오기
    getTokenExpiry: () => {
        const expiry = cookiesStorage.getItem('tokenExpiry')
        return expiry ? parseInt(expiry, 10) : null
    },

    // 모든 인증 관련 쿠키 제거
    clearAuthCookies: () => {
        cookiesStorage.removeItem('accessToken')
        cookiesStorage.removeItem('userInfo')
        cookiesStorage.removeItem('tokenExpiry')
    },

    // 아이디 기억하기 설정
    setRememberedUserId: (userId, expires = 30) => {
        cookiesStorage.setItem('rememberedUserId', userId, expires)
    },

    // 기억된 아이디 가져오기
    getRememberedUserId: () => {
        return cookiesStorage.getItem('rememberedUserId')
    },

    // 기억된 아이디 제거
    removeRememberedUserId: () => {
        cookiesStorage.removeItem('rememberedUserId')
    },

    // 언어 설정 저장
    setLanguage: (language, expires = 365) => {
        cookiesStorage.setItem('language', language, expires)
    },

    // 언어 설정 가져오기
    getLanguage: () => {
        return cookiesStorage.getItem('language')
    },

    // 테마 설정 저장
    setTheme: (theme, expires = 365) => {
        cookiesStorage.setItem('theme', theme, expires)
    },

    // 테마 설정 가져오기
    getTheme: () => {
        return cookiesStorage.getItem('theme')
    }
}

// default export로 cookiesStorage 객체를 내보냄
export default cookiesStorage