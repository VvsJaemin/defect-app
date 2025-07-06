// hooks/usePageTitle.js
import { useEffect } from 'react';

const usePageTitle = (title) => {
    useEffect(() => {
        if (title) {
            document.title = `${title} - 결함관리시스템`;
        } else {
            document.title = '결함관리시스템';
        }
    }, [title]);
};

export default usePageTitle;