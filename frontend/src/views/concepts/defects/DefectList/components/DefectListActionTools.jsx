import Button from '@/components/ui/Button'
import { TbCloudDownload, TbUserPlus } from 'react-icons/tb'
import { useNavigate } from 'react-router'
import useDefectList from '../hooks/useDefectList.js'
import { CSVLink } from 'react-csv'

const DefectListActionTools = () => {
    const navigate = useNavigate()

    const { customerList } = useDefectList()

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
            <Button
                variant="solid"
                icon={<TbUserPlus className="text-xl" />}
                    onClick={() => navigate('/defect-management/create')}
            >
                결함 등록
            </Button>
        </div>
    )
}

export default DefectListActionTools
