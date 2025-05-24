import AdaptiveCard from '@/components/shared/AdaptiveCard'
import Container from '@/components/shared/Container'
import ProjectListTable from './components/ProjectListTable.jsx'
import ProjectListActionTools from './components/ProjectListActionTools.jsx'
import ProjectListTableTools from './components/ProjectListTableTools.jsx'
import ProjectListSelected from './components/ProjectListSelected.jsx'

const ProjectList = () => {
    return (
        <>
            <Container>
                <AdaptiveCard>
                    <div className="flex flex-col gap-4">
                        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-2">
                            <h3>프로젝트 관리</h3>
                            <ProjectListActionTools />
                        </div>
                        <ProjectListTableTools />
                        <ProjectListTable />
                    </div>
                </AdaptiveCard>
            </Container>
            <ProjectListSelected />
        </>
    )
}

export default ProjectList
