import Loading from '@/components/shared/Loading'
import ProjectSection from './ProjectSection.jsx'
import useSWR from 'swr'
import { useParams } from 'react-router'
import isEmpty from 'lodash/isEmpty'
import { apiGetProjectRead } from '@/services/axios/ProjectService.js'

const ProjectDetails = () => {
    const { projectId } = useParams()

    const { data, isLoading, error } = useSWR(
        ['/projects/read', { projectId }],
        // eslint-disable-next-line no-unused-vars
        ([_, params]) => apiGetProjectRead(params),
        {
            revalidateOnFocus: false,
            revalidateIfStale: false,
            revalidateOnMount: true,
        },
    )

    if (error) {
        return (
            <div className="w-full p-5">
                <div className="text-center text-red-500">
                    프로젝트 정보를 불러오는 중 오류가 발생했습니다. 다시 시도해
                    주세요.
                </div>
            </div>
        )
    }

    return (
        <Loading loading={isLoading} spinnerClass="my-10">
            {!isEmpty(data) && (
                <div className="w-full max-w-[700px] mx-auto">
                    <ProjectSection data={data} />
                </div>
            )}
        </Loading>
    )
}

export default ProjectDetails