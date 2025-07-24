import classNames from '@/utils/classNames'
import ScrollBar from '@/components/ui/ScrollBar'
import Logo from '@/components/template/Logo'
import VerticalMenuContent from '@/components/template/VerticalMenuContent'
import { useThemeStore } from '@/store/themeStore'
import { useRouteKeyStore } from '@/store/routeKeyStore'
import { userIdStorage } from '@/utils/userIdStorage'
import navigationConfig from '@/configs/navigation.config'
import appConfig from '@/configs/app.config'
import { Link } from 'react-router'
import { useEffect, useState } from 'react'
import {
    SIDE_NAV_WIDTH,
    SIDE_NAV_COLLAPSED_WIDTH,
    SIDE_NAV_CONTENT_GUTTER,
    HEADER_HEIGHT,
    LOGO_X_GUTTER,
} from '@/constants/theme.constant'

const sideNavStyle = {
    width: SIDE_NAV_WIDTH,
    minWidth: SIDE_NAV_WIDTH,
}

const sideNavCollapseStyle = {
    width: SIDE_NAV_COLLAPSED_WIDTH,
    minWidth: SIDE_NAV_COLLAPSED_WIDTH,
}

const SideNav = ({
                     translationSetup = appConfig.activeNavTranslation,
                     background = true,
                     className,
                     contentClass,
                     mode,
                 }) => {
    const [userId, setUserId] = useState(null)

    const defaultMode = useThemeStore((state) => state.mode)
    const direction = useThemeStore((state) => state.direction)
    const sideNavCollapse = useThemeStore(
        (state) => state.layout.sideNavCollapse,
    )
    const currentRouteKey = useRouteKeyStore((state) => state.currentRouteKey)

    // 컴포넌트 마운트 시 userId 가져오기
    useEffect(() => {
        const storedUserId = userIdStorage.getUserId()
        setUserId(storedUserId)

        // 스토리지 변경 감지 (다른 탭에서 로그인/로그아웃 시)
        const handleStorageChange = (e) => {
            if (e.key === 'userId') {
                setUserId(e.newValue)
            }
        }

        window.addEventListener('storage', handleStorageChange)

        return () => {
            window.removeEventListener('storage', handleStorageChange)
        }
    }, [])

    // userId가 없으면 렌더링 안함
    if (!userId) {
        return null
    }

    return (
        <div
            style={sideNavCollapse ? sideNavCollapseStyle : sideNavStyle}
            className={classNames(
                'side-nav',
                background && 'side-nav-bg',
                !sideNavCollapse && 'side-nav-expand',
                className,
            )}
        >
            <Link
                to={appConfig.homePath}
                className="side-nav-header flex flex-row items-center justify-start"
                style={{ height: HEADER_HEIGHT }}
            >
                <Logo
                    imgClass="max-h-10"
                    mode={mode || defaultMode}
                    type="streamline"
                    className={classNames(
                        sideNavCollapse && 'ltr:ml-[11.5px] ltr:mr-[11.5px]',
                        sideNavCollapse
                            ? SIDE_NAV_CONTENT_GUTTER
                            : LOGO_X_GUTTER,
                    )}
                />
                {!sideNavCollapse && (
                    <span className="text-lg font-semibold text-gray-800 dark:text-white">
                        품질관리시스템
                    </span>
                )}
            </Link>

            <div className={classNames('side-nav-content', contentClass)}>
                <ScrollBar style={{ height: '100%' }} direction={direction}>
                    <VerticalMenuContent
                        collapsed={sideNavCollapse}
                        navigationTree={navigationConfig}
                        routeKey={currentRouteKey}
                        direction={direction}
                        translationSetup={translationSetup}
                        userId={userId} // userId 전달
                    />
                </ScrollBar>
            </div>
        </div>
    )
}

export default SideNav