import Header from '@/components/template/Header'
import UserProfileDropdown from '@/components//template/UserProfileDropdown'
import HeaderLogo from '@/components/template/HeaderLogo'
import MobileNav from '@/components/template/MobileNav'
import HorizontalNav from '@/components/template/HorizontalNav'
import LayoutBase from '@/components//template/LayoutBase'
import useResponsive from '@/utils/hooks/useResponsive'
import { LAYOUT_TOP_BAR_CLASSIC } from '@/constants/theme.constant'
import SidePanel from '@/components/template/SidePanel/SidePanel.jsx'

const TopBarClassic = ({ children }) => {
    const { larger, smaller } = useResponsive()

    return (
        <LayoutBase
            type={LAYOUT_TOP_BAR_CLASSIC}
            className="app-layout-top-bar-classic flex flex-auto flex-col min-h-screen"
        >
            <div className="flex flex-auto min-w-0">
                <div className="flex flex-col flex-auto min-h-screen min-w-0 relative w-full">
                    <Header
                        container
                        className="shadow-sm dark:shadow-2xl"
                        headerStart={
                            <>
                                {smaller.lg && <MobileNav />}
                                <HeaderLogo />
                            </>
                        }
                        headerMiddle={<>{larger.lg && <HorizontalNav />}</>}
                        headerEnd={
                            <>
                                <SidePanel />
                                <UserProfileDropdown hoverable={false} />
                            </>
                        }
                    />
                    {children}
                </div>
            </div>
        </LayoutBase>
    )
}

export default TopBarClassic
