import LayoutBase from '@/components//template/LayoutBase'
import { LAYOUT_BLANK } from '@/constants/theme.constant'
import SidePanel from '@/components/template/SidePanel/SidePanel.jsx'
import Header from '@/components/template/Header.jsx'
import MobileNav from '@/components/template/MobileNav.jsx'
import UserProfileDropdown from '@/components/template/UserProfileDropdown.jsx'

const Blank = ({ children }) => {
    return (
        <LayoutBase
            type={LAYOUT_BLANK}
            className="app-layout-blank flex flex-auto flex-col h-[100vh]"
        >
            <Header
                className="shadow-sm dark:shadow-2xl"
                headerEnd={
                    <>
                        <SidePanel />
                        <UserProfileDropdown hoverable={false} />
                    </>
                }
            />
            <div className="flex min-w-0 w-full flex-1">
                {children}
            </div>
        </LayoutBase>
    )
}

export default Blank
