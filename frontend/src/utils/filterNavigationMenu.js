export function filterNavigationMenu(menuList, userAuthority = []) {
    // 권한 체크 함수
    const hasAuthority = (menu, authority) => {
        if (!menu.authority || menu.authority.length === 0) return true
        if (!authority || authority.length === 0) return false
        // 둘 다 배열인 것이 권장됩니다!
        return menu.authority.some(a => authority.includes(a))

    }
    return menuList
        .map(menu => {
            // 서브메뉴도 필터링
            const filteredSubMenu = menu.subMenu && menu.subMenu.length > 0
                ? filterNavigationMenu(menu.subMenu, userAuthority)
                : []
            // 본인 혹은 서브 중에 접근 가능한 게 있을 경우 유지
            if (hasAuthority(menu, userAuthority) || filteredSubMenu.length > 0) {
                return {
                    ...menu,
                    subMenu: filteredSubMenu,
                }
            }
            // 노출하지 않음
            return null
        })
        .filter(Boolean)
}
