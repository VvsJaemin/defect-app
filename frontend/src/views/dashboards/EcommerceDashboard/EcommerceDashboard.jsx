import Loading from '@/components/shared/Loading'
import Overview from './components/Overview'
import CustomerDemographic from './components/CustomerDemographic'
import useSWR from 'swr'
import { apiPrefix } from '@/configs/endpoint.config.js'
import axios from 'axios'

const DefectDashboard = () => {
    const { data, isLoading } = useSWR(
        ['/defects/dashboard/list'],
        () =>
            axios
                .get(`${apiPrefix}/defects/dashboard/list`, {
                    withCredentials: true,
                })
                .then((res) => res.data),
        {
            revalidateOnFocus: false,
            revalidateIfStale: false,
            revalidateOnReconnect: false,
        },
    )

    return (
        <Loading loading={isLoading}>
            {data && (
                <div>
                    <div className="flex flex-col gap-4 max-w-full overflow-x-hidden">
                        <div className="flex flex-col xl:flex-row gap-4">
                            <div className="flex flex-col gap-4 flex-1 xl:col-span-3">
                                <Overview data={data.statisticData}  weeklyData={data.weeklyStats} />
                                <CustomerDemographic
                                    data={data.statisticData}
                                />

                            </div>
                            {/*<div className="flex flex-col gap-4 2xl:min-w-[360px]">*/}
                            {/*    <SalesTarget data={data.salesTarget} />*/}
                            {/*    <TopProduct data={data.topProduct} />*/}
                            {/*    <RevenueByChannel*/}
                            {/*        data={data.revenueByChannel}*/}
                            {/*    />*/}
                            {/*</div>*/}
                        </div>

                        {/*<RecentOrder data={data.recentOrders} />*/}
                    </div>
                </div>
            )}
        </Loading>
    )
}

export default DefectDashboard