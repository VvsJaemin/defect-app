// hooks/usePageTitle.js
import { useEffect } from 'react';

const usePageTitle = (title) => {
    useEffect(() => {
        if (title) {
            document.title = `${title} - 품질관리시스템`;
        } else {
            document.title = '품질관리시스템';
        }
    }, [title]);
};

export default usePageTitle;