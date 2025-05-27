import { TbSearch, TbRefresh } from 'react-icons/tb'
import { Button } from '@/components/ui/Button'
import { useState } from 'react'
import Input from '@/components/ui/Input/Input.jsx'
import Select from '@/components/ui/Select/Select.jsx'
import isEmpty from 'lodash/isEmpty'
import Alert from '@/components/ui/Alert/Alert.jsx'

const ProjectListSearch = (props) => {
    const { onInputChange, onReset, ref } = props
    const [searchValue, setSearchValue] = useState('')
    const [searchType, setSearchType] = useState({ value: '', label: '선택하세요' }) // 기본 검색 타입
    const [alertMessage, setAlertMessage] = useState('')
    const [showAlert, setShowAlert] = useState(false)

    // 검색 타입 옵션
    const searchOptions = [
        { value: '', label: '선택하세요' },
        { value: 'projectName', label: '프로젝트명' },
        { value: 'urlInfo', label: 'URL' },
        { value: 'customerName', label: '고객사' },
        { value: 'statusCode', label: '상태' }
    ]

    // Alert 닫기 핸들러
    const handleAlertClose = () => {
        setShowAlert(false)
    }

    // 검색 버튼 클릭 핸들러
    const handleSearch = () => {
        // 검색 타입이 선택되지 않았을 경우
        if (isEmpty(searchType.value)) {
            setAlertMessage('검색 유형을 선택해주세요.');
            setShowAlert(true);
            return;
        }


        // 모든 조건이 충족되면 검색 실행
        onInputChange({
            type: searchType.value,
            value: searchValue
        });
    }

    // 초기화 버튼 클릭 핸들러
    const handleReset = () => {
        // 내부 상태 초기화
        setSearchValue('')
        setSearchType({ value: '', label: '선택하세요' })

        // 부모 컴포넌트에 초기화 이벤트 전달
        onReset && onReset()
    }

    // Enter 키 이벤트 핸들러 추가
    const handleKeyPress = (e) => {
        if (e.key === 'Enter') {
            handleSearch()
        }
    }

    // 검색어 입력 핸들러
    const handleInputChange = (e) => {
        setSearchValue(e.target.value)
    }

    // 검색 타입 변경 핸들러
    const handleSelectChange = (option) => {
        setSearchType(option)
    }

    return (
        <div>
            {showAlert && (
                <Alert
                    type="warning"
                    closable
                    showIcon
                    onClose={handleAlertClose}
                >
                    {alertMessage}
                </Alert>
            )}

            <div className="flex gap-2 mt-3">
                {/* 검색 타입 선택 */}
                <div className="w-40">
                    <Select
                        value={searchType}
                        options={searchOptions}
                        onChange={handleSelectChange}
                        isSearchable={false}
                    />
                </div>

                {/* 검색어 입력 */}
                <div className="flex-1">
                    <Input
                        ref={ref}
                        placeholder="검색어를 입력하세요."
                        suffix={<TbSearch className="text-lg" />}
                        onChange={handleInputChange}
                        value={searchValue}
                        onKeyPress={handleKeyPress}
                    />
                </div>

                {/* 검색 버튼 */}
                <Button variant="solid" onClick={handleSearch} icon={<TbSearch className="text-lg" />}>
                    검색
                </Button>

                {/* 초기화 버튼 */}
                <Button
                    variant="outline"
                    onClick={handleReset}
                    icon={<TbRefresh className="text-lg" />}
                >
                    초기화
                </Button>
            </div>
        </div>
    )
}

export default ProjectListSearch