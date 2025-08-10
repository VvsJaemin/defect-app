import DefectOverview from './components/DefectOverview.jsx'
import DefectDemographic from './components/DefectDemographic.jsx'
import useSWR from 'swr'
import { apiGetDefectDashboard } from '@/services/DashboardService'

const DefectDashboard = () => {
    const { data, error } = useSWR(
        '/defects/dashboard/list',
        apiGetDefectDashboard,
        {
            revalidateOnFocus: false,
            revalidateIfStale: false,
            revalidateOnReconnect: false,
        },
    )

    if (error) {
        console.error('DefectDashboard 오류:', error)
    }

    return (
        <div>
            <div className="flex flex-col gap-4 max-w-full overflow-x-hidden">
                <div className="flex flex-col xl:flex-row gap-4">
                    <div className="flex flex-col gap-4 flex-1 xl:col-span-3">
                        {data && (
                            <>
                                <DefectOverview data={data.statisticData} weeklyData={data.weeklyStats} />
                                <DefectDemographic data={data.statisticData} />
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
    )
}

export default DefectDashboard