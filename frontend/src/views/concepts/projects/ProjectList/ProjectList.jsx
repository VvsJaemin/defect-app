import AdaptiveCard from '@/components/shared/AdaptiveCard'
import ProjectListTable from './components/ProjectListTable.jsx'
import ProjectListActionTools from './components/ProjectListActionTools.jsx'
import ProjectListTableTools from './components/ProjectListTableTools.jsx'
import ProjectListSelected from './components/ProjectListSelected.jsx'

const ProjectList = () => {
    return (
        <>
            <AdaptiveCard>
                <div className="flex flex-col gap-4 max-w-full overflow-x-hidden">
                    <div className="flex flex-col xl:flex-row gap-4">
                        <div className="flex flex-col gap-4 flex-1 xl:col-span-3">
                            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-2">
                                <h3>프로젝트 관리</h3>
                                <ProjectListActionTools />
                            </div>
                            <ProjectListTableTools />
                            <ProjectListTable />
                        </div>
                    </div>
                </div>
            </AdaptiveCard>
            <ProjectListSelected />
        </>
    )
}

export default ProjectList