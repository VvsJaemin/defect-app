export const userIdStorage = {
    setUserId: (userId) => {
        if (userId) {
            localStorage.setItem('userId', userId)
            console.log('UserId stored in localStorage:', userId)
        }
    },

    getUserId: () => {
        const userId = localStorage.getItem('userId')
        console.log('UserId retrieved from localStorage:', userId)
        return userId
    },

    removeUserId: () => {
        localStorage.removeItem('userId')
        console.log('UserId removed from localStorage')
    }
}