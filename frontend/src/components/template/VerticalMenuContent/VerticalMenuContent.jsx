
import { useState, useEffect, Fragment } from 'react'
import Menu from '@/components/ui/Menu'
import VerticalSingleMenuItem from './VerticalSingleMenuItem'
import VerticalCollapsedMenuItem from './VerticalCollapsedMenuItem'
import AuthorityCheck from '@/components/shared/AuthorityCheck'
import { themeConfig } from '@/configs/theme.config'
import {
    NAV_ITEM_TYPE_TITLE,
    NAV_ITEM_TYPE_COLLAPSE,
    NAV_ITEM_TYPE_ITEM,
} from '@/constants/navigation.constant'
import useMenuActive from '@/utils/hooks/useMenuActive'
import useTranslation from '@/utils/hooks/useTranslation'

const { MenuGroup } = Menu

const MAX_CASCADE_LEVEL = 2

const VerticalMenuContent = (props) => {
    const {
        collapsed,
        routeKey,
        navigationTree = [],
        onMenuItemClick,
        onMenuClick, // 새로 추가된 prop
        direction = themeConfig.direction,
        translationSetup,
        userAuthority,
        userId,
    } = props

    const { t } = useTranslation(!translationSetup)

    const [defaulExpandKey, setDefaulExpandKey] = useState([])

    const { activedRoute } = useMenuActive(navigationTree, routeKey)

    useEffect(() => {
        if (activedRoute?.parentKey) {
            setDefaulExpandKey([activedRoute?.parentKey])
        }
    }, [activedRoute?.parentKey])

    const handleLinkClick = (nav) => {
        // 기존 메뉴 아이템 클릭 핸들러
        onMenuItemClick?.()

        // 새로운 메뉴 클릭 핸들러 (경로를 전달)
        if (onMenuClick && nav?.path) {
            onMenuClick(nav.path)
        }
    }

    const renderNavigation = (navTree, cascade = 0, indent) => {
        const nextCascade = cascade + 1

        return (
            <>
                {navTree.map((nav) => (
                    <Fragment key={nav.key}>
                        {nav.type === NAV_ITEM_TYPE_ITEM && (
                            <VerticalSingleMenuItem
                                key={nav.key}
                                currentKey={activedRoute?.key}
                                parentKeys={defaulExpandKey}
                                nav={nav}
                                sideCollapsed={collapsed}
                                direction={direction}
                                indent={indent}
                                renderAsIcon={cascade <= 0}
                                showIcon={cascade <= 0}
                                userAuthority={userAuthority}
                                userId={userId}
                                showTitle={
                                    collapsed
                                        ? cascade >= 1
                                        : cascade <= MAX_CASCADE_LEVEL
                                }
                                t={t}
                                onLinkClick={() => handleLinkClick(nav)}
                            />
                        )}
                        {nav.type === NAV_ITEM_TYPE_COLLAPSE && (
                            <VerticalCollapsedMenuItem
                                key={nav.key}
                                currentKey={activedRoute?.key}
                                parentKeys={defaulExpandKey}
                                nav={nav}
                                sideCollapsed={collapsed}
                                direction={direction}
                                indent={nextCascade >= MAX_CASCADE_LEVEL}
                                dotIndent={nextCascade >= MAX_CASCADE_LEVEL}
                                renderAsIcon={nextCascade <= 1}
                                userAuthority={userAuthority}
                                userId={userId}
                                t={t}
                                onLinkClick={() => handleLinkClick(nav)}
                            >
                                {nav.subMenu &&
                                    nav.subMenu.length > 0 &&
                                    renderNavigation(
                                        nav.subMenu,
                                        nextCascade,
                                        true,
                                    )}
                            </VerticalCollapsedMenuItem>
                        )}
                        {nav.type === NAV_ITEM_TYPE_TITLE && (
                            <AuthorityCheck
                                userAuthority={userAuthority}
                                authority={nav.authority}
                            >
                                <MenuGroup
                                    key={nav.key}
                                    label={t(nav.translateKey) || nav.title}
                                >
                                    {nav.subMenu &&
                                        nav.subMenu.length > 0 &&
                                        renderNavigation(
                                            nav.subMenu,
                                            cascade,
                                            false,
                                        )}
                                </MenuGroup>
                            </AuthorityCheck>
                        )}
                    </Fragment>
                ))}
            </>
        )
    }

    return (
        <Menu
            className="px-4 pb-4"
            sideCollapsed={collapsed}
            defaultActiveKeys={activedRoute?.key ? [activedRoute.key] : []}
            defaultExpandedKeys={defaulExpandKey}
            defaultCollapseActiveKeys={
                activedRoute?.parentKey ? [activedRoute.parentKey] : []
            }
        >
            {renderNavigation(navigationTree, 0)}
        </Menu>
    )
}

export default VerticalMenuContent