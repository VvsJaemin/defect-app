import Loading from '@/components/shared/Loading'
import ProfileSection from './ProfileSection'
import useSWR from 'swr'
import { useParams } from 'react-router'
import isEmpty from 'lodash/isEmpty'
import { apiGetCustomer } from '@/services/UserService.js'

const CustomerDetails = () => {
    const { userId } = useParams()

    const { data, isLoading, error } = useSWR(
        ['/users/read', { userId }],
        // eslint-disable-next-line no-unused-vars
        ([_, params]) => apiGetCustomer(params),
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
                    사용자 정보를 불러오는 중 오류가 발생했습니다. 다시 시도해 주세요.
                </div>
            </div>
        )
    }

    return (
        <Loading loading={isLoading} spinnerClass="my-10">
            {!isEmpty(data) && (
                <div className="w-full max-w-[700px] mx-auto">
                    <ProfileSection data={data} />
                </div>
            )}
        </Loading>
    )
}

export default CustomerDetails