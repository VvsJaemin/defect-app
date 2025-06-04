import Loading from '@/components/shared/Loading'
import DefectSection from './DefectSection.jsx'
import useSWR from 'swr'
import { useParams } from 'react-router'
import isEmpty from 'lodash/isEmpty'
import { apiGetProjectRead } from '@/services/ProjectService.js'
import { apiGetDefectRead } from '@/services/DefectService.js'

const DefectDetails = () => {
    const { defectId } = useParams()

    const { data, isLoading, error } = useSWR(
        ['/defects/read', { defectId }],
        // eslint-disable-next-line no-unused-vars
        ([_, params]) => apiGetDefectRead(params),
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
                    결함 목록을 불러오는 중 오류가 발생했습니다. 다시 시도해
                    주세요.
                </div>
            </div>
        )
    }

    return (
        <Loading loading={isLoading} spinnerClass="my-10">
            {!isEmpty(data) && (
                <div className="w-full max-w-[700px] mx-auto">
                    <DefectSection data={data} />
                </div>
            )}
        </Loading>
    )
}

export default DefectDetails