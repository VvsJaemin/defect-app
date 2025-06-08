import Button from '@/components/ui/Button'
import { TbUserPlus } from 'react-icons/tb'
import { useNavigate } from 'react-router'
import useDefectList from '../hooks/useDefectList.js'
import { useAuth } from '@/auth/index.js'

const DefectListActionTools = () => {
    const navigate = useNavigate()

    const { customerList } = useDefectList()
    const { user } = useAuth()

    // 사용자 권한 확인 - MG 또는 QA인 경우에만 수정 권한 부여
    const canRegisterDefect = user?.userSeCd === 'MG' || user?.userSeCd === 'QA'
    return (
        <div className="flex flex-col md:flex-row gap-3">
            {/*<CSVLink*/}
            {/*    className="w-full"*/}
            {/*    filename="customerList.csv"*/}
            {/*    data={customerList}*/}
            {/*>*/}
            {/*    <Button*/}
            {/*        icon={<TbCloudDownload className="text-xl" />}*/}
            {/*        className="w-full"*/}
            {/*    >*/}
            {/*        Download*/}
            {/*    </Button>*/}
            {/*</CSVLink>*/}
            {canRegisterDefect && (
                <Button
                    variant="solid"
                    icon={<TbUserPlus className="text-xl" />}
                    onClick={() => navigate('/defect-management/create')}
                >
                    결함 등록
                </Button>
            )}
        </div>
    )
}

export default DefectListActionTools
