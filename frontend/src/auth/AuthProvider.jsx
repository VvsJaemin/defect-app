import { useRef, useImperativeHandle, useEffect } from 'react';
import AuthContext from './AuthContext';
import appConfig from '@/configs/app.config';
import { useSessionUser } from '@/store/authStore';
import { apiSignIn, apiSignOut, apiSignUp } from '@/services/AuthService';
import { REDIRECT_URL_KEY } from '@/constants/app.constant';
import { useNavigate } from 'react-router';

const IsolatedNavigator = ({ ref }) => {
    const navigate = useNavigate();

    useImperativeHandle(ref, () => ({
        navigate,
    }), [navigate]);

    return <></>;
};

function AuthProvider({ children }) {
    const signedIn = useSessionUser((state) => state.session.signedIn);
    const user = useSessionUser((state) => state.user);
    const setUser = useSessionUser((state) => state.setUser);
    const setSessionSignedIn = useSessionUser((state) => state.setSessionSignedIn);
    const checkSession = useSessionUser((state) => state.checkSession);
    const reset = useSessionUser((state) => state.reset);
    const isLoggedOutManually = useSessionUser((state) => state.isLoggedOutManually);

    const authenticated = Boolean(signedIn);

    const navigatorRef = useRef(null);

    // 초기 세션 확인
    useEffect(() => {
        if (!isLoggedOutManually) {
            checkSession();
        }
    }, [checkSession, isLoggedOutManually]);

    const redirect = () => {
        const search = window.location.search;
        const params = new URLSearchParams(search);
        const redirectUrl = params.get(REDIRECT_URL_KEY) || appConfig.authenticatedEntryPath;

        navigatorRef.current?.navigate(redirectUrl);
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
        reset(); // Zustand 상태 초기화
    };

    const signIn = async (values) => {
        try {
            const resp = await apiSignIn(values);
            console.log('Sign-in response:', JSON.stringify(resp));
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
            console.error('Sign-in error:', error);
            return {
                status: 'failed',
                message: error?.response?.data?.message || 'Sign-in failed',
            };
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
            navigatorRef.current?.navigate(appConfig.unAuthenticatedEntryPath || '/');
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
            <IsolatedNavigator ref={navigatorRef} />
        </AuthContext.Provider>
    );
}

export default AuthProvider;