// components/common/PageTitleManager.jsx
import { useEffect } from 'react';
import { useLocation } from 'react-router';
import { getPageTitleByPath, customPageTitles } from '@/utils/pageTitle';

const PageTitleManager = () => {
    const location = useLocation();

    useEffect(() => {
        const pathname = location.pathname;

        // 커스텀 타이틀이 있는지 먼저 확인
        let title = customPageTitles[pathname];

        // 커스텀 타이틀이 없으면 네비게이션 설정에서 찾기
        if (!title) {
            title = getPageTitleByPath(pathname);
        }

        // 동적 경로 처리 (예: /user-management/details/12345)
        if (!title) {
            if (pathname.includes('/user-management/details/')) {
                title = '사용자 상세';
            } else if (pathname.includes('/user-management/update/')) {
                title = '사용자 수정';
            } else if (pathname.includes('/project-management/details/')) {
                title = '프로젝트 상세';
            } else if (pathname.includes('/project-management/update/')) {
                title = '프로젝트 수정';
            } else if (pathname.includes('/defect-management/details/')) {
                title = '결함 상세';
            } else if (pathname.includes('/defect-management/update/')) {
                title = '결함 수정';
            }
        }

        // 타이틀 설정
        if (title) {
            document.title = `${title} - 결함관리시스템`;
        } else {
            document.title = '결함관리시스템';
        }
    }, [location.pathname]);

    return null; // 이 컴포넌트는 렌더링하지 않음
};

export default PageTitleManager;