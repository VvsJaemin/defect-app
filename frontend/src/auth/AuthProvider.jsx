import { useEffect } from 'react';
import AuthContext from './AuthContext';
import appConfig from '@/configs/app.config';
import { useSessionUser } from '@/store/authStore';
import { apiSignIn, apiSignOut, apiSignUp } from '@/services/AuthService';
import { REDIRECT_URL_KEY } from '@/constants/app.constant';
import { useNavigate } from 'react-router';

function AuthProvider({ children }) {
    const signedIn = useSessionUser((state) => state.session.signedIn);
    const user = useSessionUser((state) => state.user);
    const setUser = useSessionUser((state) => state.setUser);
    const setSessionSignedIn = useSessionUser((state) => state.setSessionSignedIn);
    const checkSession = useSessionUser((state) => state.checkSession);
    const reset = useSessionUser((state) => state.reset);
    const isLoggedOutManually = useSessionUser((state) => state.isLoggedOutManually);
    const setNavigator = useSessionUser((state) => state.setNavigator);

    const authenticated = Boolean(signedIn);

    const navigate = useNavigate();

    // zustand store에 네비게이션 함수 등록
    useEffect(() => {
        setNavigator(navigate);
        return () => setNavigator(null); // cleanup
    }, [setNavigator, navigate]);

    useEffect(() => {
        checkSession()
    }, [checkSession])

    const redirect = () => {
        const search = window.location.search;
        const params = new URLSearchParams(search);
        const redirectUrl = params.get(REDIRECT_URL_KEY) || appConfig.authenticatedEntryPath;

        navigate(redirectUrl);
    };

    const handleSignIn = (user) => {
        if (!user || !user.userId) {
            console.error('Invalid user data:', user);
            return;
        }
        setSessionSignedIn(true);
        setUser(user);
    };

    const handleSignOut = () => {
        reset();
    };

    const signIn = async (values) => {
        try {
            const resp = await apiSignIn(values);
            if (resp && resp.userId) {
                handleSignIn(resp);
                redirect();
                return {
                    status: 'success',
                    message: '',
                };
            }
            return {
                status: 'failed',
                message: 'Unable to sign in: No user data received',
            };
        } catch (error) {
            console.log(error?.response?.data);
            return {
                status: 'failed',
                message: error?.response?.data,
            }
        }
    };

    const signUp = async (values) => {
        try {
            const resp = await apiSignUp(values);
            console.log('Sign-up response:', JSON.stringify(resp));
            if (resp && resp.userId) {
                handleSignIn(resp);
                redirect();
                return {
                    status: 'success',
                    message: '',
                };
            }
            return {
                status: 'failed',
                message: 'Unable to sign up: No user data received',
            };
        } catch (error) {
            console.error('Sign-up error:', error);
            return {
                status: 'failed',
                message: error?.response?.data?.message || 'Sign-up failed',
            };
        }
    };

    const signOut = async () => {
        try {
            await apiSignOut();
        } catch (error) {
            console.error('Sign-out error:', error);
        } finally {
            handleSignOut();
            navigate(appConfig.unAuthenticatedEntryPath || '/');
        }
    };

    const oAuthSignIn = (callback) => {
        callback({
            onSignIn: handleSignIn,
            redirect,
        });
    };

    return (
        <AuthContext.Provider
            value={{
                authenticated,
                user,
                signIn,
                signUp,
                signOut,
                oAuthSignIn,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export default AuthProvider;