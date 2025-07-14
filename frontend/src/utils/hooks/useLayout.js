import { createContext, useContext } from 'react'

export const LayoutContext = createContext(undefined)

const useLayout = () => {
    const context = useContext(LayoutContext)
    if (!context) {
        // 에러를 던지지 않고 기본값 반환
        console.warn('useLayout must be used within a LayoutProvider, returning default values')
        return {
            pageContainerReassemble: null,
            layoutType: 'default',
            // 기타 필요한 기본값들 추가
        }
    }
    return context
}


export default useLayout
