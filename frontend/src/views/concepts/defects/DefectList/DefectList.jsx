import AdaptiveCard from '@/components/shared/AdaptiveCard'
import Container from '@/components/shared/Container'
import DefectListTable from './components/DefectListTable.jsx'
import DefectListActionTools from './components/DefectListActionTools.jsx'
import DefectListTableTools from './components/DefectListTableTools.jsx'
import DefectListSelected from './components/DefectListSelected.jsx'
import { useLocation } from 'react-router'

const DefectList = () => {
    const location = useLocation()

    const isAssignedPage = location.pathname === '/defect-management/assigned'
    const isInProgressPage = location.pathname === '/defect-management/in-progress'
    const isCompletedPage = location.pathname === '/defect-management/completed'
    const isTodoPage = location.pathname === '/defect-management/todo'

    const getPageTitle = () => {
        if (isAssignedPage) {
            return '내게 할당된 결함'
        }
        if (isInProgressPage) {
            return '조치중 결함'
        }
        if (isCompletedPage) {
            return '조치완료 결함'
        }
        if (isTodoPage) {
            return 'To-Do'
        }
        return '품질 관리'
    }

    return (
        <>
            <div className="w-full px-4">
                <AdaptiveCard className="w-full">
                    <div className="flex flex-col gap-4 w-full">
                        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-2">
                            <h3>{getPageTitle()}</h3>
                            <DefectListActionTools />
                        </div>
                        <DefectListTableTools />
                        <div className="w-full">
                            <DefectListTable path={location.pathname} />
                        </div>
                    </div>
                </AdaptiveCard>
            </div>
            <DefectListSelected />
        </>
    )
}

export default DefectList