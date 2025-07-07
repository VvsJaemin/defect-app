import { BrowserRouter } from 'react-router'
import Theme from '@/components/template/Theme'
import Layout from '@/components/layouts'
import { AuthProvider } from '@/auth'
import Views from '@/views'
import PageTitleManager from '@/components/common/PageTitleManager'

// if (appConfig.enableMock) {
//     import('./mock')
// }

const App = () => {
    return (
        <Theme>
            <BrowserRouter>
                <PageTitleManager />
                <AuthProvider>
                    <Layout>
                        <Views />
                    </Layout>
                </AuthProvider>
            </BrowserRouter>
        </Theme>
    )
}

export default App