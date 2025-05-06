import DebouceInput from '@/components/shared/DebouceInput'
import { TbSearch } from 'react-icons/tb'

const CustomerListSearch = (props) => {
    const { onInputChange, ref } = props

    return (
        <DebouceInput
            ref={ref}
            placeholder="검색어를 입력하세요."
            suffix={<TbSearch className="text-lg" />}
            onChange={(e) => onInputChange(e.target.value)}
        />
    )
}

export default CustomerListSearch
