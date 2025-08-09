import classNames from '@/utils/classNames'
import ScrollBar from '@/components/ui/ScrollBar'
import Logo from '@/components/template/Logo'
import VerticalMenuContent from '@/components/template/VerticalMenuContent'
import { useThemeStore } from '@/store/themeStore'
import { useSessionUser } from '@/store/authStore'
import { useRouteKeyStore } from '@/store/routeKeyStore'
import navigationConfig from '@/configs/navigation.config'
import appConfig from '@/configs/app.config'
import { useNavigate } from 'react-router'
import { mutate } from 'swr'
import {
    SIDE_NAV_WIDTH,
    SIDE_NAV_COLLAPSED_WIDTH,
    SIDE_NAV_CONTENT_GUTTER,
    HEADER_HEIGHT,
    LOGO_X_GUTTER,
} from '@/constants/theme.constant'
import { filterNavigationMenu } from '@/utils/filterNavigationMenu' // filter 함수 import

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
    const defaultMode = useThemeStore((state) => state.mode)
    const direction = useThemeStore((state) => state.direction)
    const sideNavCollapse = useThemeStore(
        (state) => state.layout.sideNavCollapse,
    )
    const navigate = useNavigate()

    const currentRouteKey = useRouteKeyStore((state) => state.currentRouteKey)
    const user = useSessionUser((state) => state.user)
    // 사용자 정보 없으면 렌더링 안함
    if (!user || !user.userId) {
        return null
    }
    const userAuthority = user.userSeCd
    const userId = user.userId

    // 권한별로 메뉴 필터링
    const filteredNavigationConfig = filterNavigationMenu(navigationConfig, userAuthority)

    // 대시보드 갱신 함수
    const refreshDashboardData = () => {
        // SWR 캐시를 갱신하여 데이터를 새로 로드
        mutate('/defects/dashboard/list')

        // 커스텀 이벤트도 함께 발생시켜 다른 컴포넌트가 반응할 수 있도록 함
        window.dispatchEvent(new CustomEvent('refreshDashboard'))
    }

    // 품질관리시스템 클릭 시 대시보드 갱신
    const handleDashboardRefresh = () => {
        navigate(appConfig.homePath)
        refreshDashboardData()
    }

    // 메뉴 클릭 핸들러
    const handleMenuClick = (path) => {
        // home 경로인 경우 대시보드 데이터 갱신
        if (path === appConfig.homePath) {
            refreshDashboardData()
        }
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
            <div
                onClick={handleDashboardRefresh}
                className="side-nav-header flex flex-row items-center justify-start cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
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
            </div>

            <div className={classNames('side-nav-content', contentClass)}>
                <ScrollBar style={{ height: '100%' }} direction={direction}>
                    <VerticalMenuContent
                        collapsed={sideNavCollapse}
                        navigationTree={filteredNavigationConfig}
                        routeKey={currentRouteKey}
                        direction={direction}
                        translationSetup={translationSetup}
                        userAuthority={userAuthority}
                        userId={userId}
                        onMenuClick={handleMenuClick} // 메뉴 클릭 핸들러 전달
                    />
                </ScrollBar>
            </div>
        </div>
    )
}

export default SideNav