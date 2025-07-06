// utils/pageTitle.js
import navigationConfig from '@/configs/navigation.config'

// 경로별 페이지 타이틀 맵핑
export const getPageTitleByPath = (pathname) => {
    // 정확한 경로 매칭
    const findTitleInConfig = (config, path) => {
        for (const item of config) {
            // 현재 아이템의 경로가 일치하는지 확인
            if (item.path === path) {
                return item.title;
            }

            // 동적 경로 처리 (예: /user-management/update/:userId)
            if (item.path.includes(':')) {
                const pathPattern = item.path.replace(/:[^/]+/g, '[^/]+');
                const regex = new RegExp(`^${pathPattern}$`);
                if (regex.test(path)) {
                    return item.title;
                }
            }

            // 서브메뉴가 있는 경우 재귀 검색
            if (item.subMenu && item.subMenu.length > 0) {
                const subTitle = findTitleInConfig(item.subMenu, path);
                if (subTitle) {
                    return subTitle;
                }
            }
        }
        return null;
    };

    return findTitleInConfig(navigationConfig, pathname);
};

// 특정 경로별 커스텀 타이틀 매핑
export const customPageTitles = {
    '/home': 'Home',
    '/user-management': '사용자 관리',
    '/user-management/create': '사용자 등록',
    '/project-management': '프로젝트 관리',
    '/project-management/create': '프로젝트 등록',
    '/defect-management': '결함 관리',
    '/defect-management/create': '결함 등록',
    '/defect-management/assigned': '내게 할당된 결함',
    '/defect-management/in-progress': '조치중 결함',
    '/defect-management/completed': '조치완료 결함',
    '/defect-management/todo': 'To-Do',
};