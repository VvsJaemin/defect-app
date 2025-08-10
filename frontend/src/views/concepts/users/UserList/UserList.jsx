import AdaptiveCard from '@/components/shared/AdaptiveCard'
import UserListTable from './components/UserListTable.jsx'
import UserListActionTools from './components/UserListActionTools.jsx'
import UserListTableTools from './components/UserListTableTools.jsx'
import UserListSelected from './components/UserListSelected.jsx'

const UserList = () => {
    return (
        <>
            <AdaptiveCard>
                <div className="flex flex-col gap-4 max-w-full overflow-x-hidden">
                    <div className="flex flex-col xl:flex-row gap-4">
                        <div className="flex flex-col gap-4 flex-1 xl:col-span-3">
                            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-2">
                                <h3>사용자 관리</h3>
                                <UserListActionTools />
                            </div>
                            <UserListTableTools />
                            <UserListTable />
                        </div>
                    </div>
                </div>
            </AdaptiveCard>
            <UserListSelected />
        </>
    )
}

export default UserList