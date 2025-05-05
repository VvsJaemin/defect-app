import ConfigProvider from '@/components/ui/ConfigProvider'
import { themeConfig } from '@/configs/theme.config'
import useDarkMode from '@/utils/hooks/useDarkMode'
import useThemeSchema from '@/utils/hooks/useThemeSchema'
import useLocale from '@/utils/hooks/useLocale'
import useDirection from '@/utils/hooks/useDirection'

const Theme = (props) => {
    useThemeSchema()
    useDarkMode()
    useDirection()

    const { locale } = useLocale()

    return (
        <ConfigProvider
            value={{
                locale: locale,
                ...themeConfig,
            }}
        >
            {props.children}
        </ConfigProvider>
    )
}

export default Theme
