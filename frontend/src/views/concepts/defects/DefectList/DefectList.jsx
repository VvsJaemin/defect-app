import AdaptiveCard from '@/components/shared/AdaptiveCard'
import Container from '@/components/shared/Container'
import DefectListTable from './components/DefectListTable.jsx'
import DefectListActionTools from './components/DefectListActionTools.jsx'
import DefectListTableTools from './components/DefectListTableTools.jsx'
import DefectListSelected from './components/DefectListSelected.jsx'

const DefectList = () => {
    return (
        <>
            <Container>
                <AdaptiveCard>
                    <div className="flex flex-col gap-4">
                        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-2">
                            <h3>결함 관리</h3>
                            <DefectListActionTools />
                        </div>
                        <DefectListTableTools />
                        <DefectListTable />
                    </div>
                </AdaptiveCard>
            </Container>
            <DefectListSelected />
        </>
    )
}

export default DefectList
