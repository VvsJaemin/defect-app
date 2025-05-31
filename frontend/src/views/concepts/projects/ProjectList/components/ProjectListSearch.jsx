import { TbSearch, TbRefresh } from 'react-icons/tb'
import { Button } from '@/components/ui/Button'
import { useState, useEffect } from 'react'
import Input from '@/components/ui/Input/Input.jsx'
import Select from '@/components/ui/Select/Select.jsx'
import isEmpty from 'lodash/isEmpty'
import Alert from '@/components/ui/Alert/Alert.jsx'

const ProjectListSearch = (props) => {
    const { onInputChange, onReset, ref } = props
    const [searchValue, setSearchValue] = useState('')
    const [searchType, setSearchType] = useState({ value: '', label: '선택하세요' })
    const [projectState, setProjectState] = useState({ value: '', label: '상태 선택' })
    const [alertMessage, setAlertMessage] = useState('')
    const [showAlert, setShowAlert] = useState(false)

    // 검색 타입 옵션
    const searchOptions = [
        { value: '', label: '선택하세요' },
        { value: 'projectName', label: '프로젝트명' },
        { value: 'urlInfo', label: 'URL' },
        { value: 'customerName', label: '고객사' },
        { value: 'projectState', label: '상태' }
    ]

    // 프로젝트 상태 옵션
    const projectStateOptions = [
        { value: '', label: '선택하세요' },
        { value: 'DEV', label: '개발 버전' },
        { value: 'OPERATE', label: '운영 버전' },
        { value: 'TEST', label: '테스트 버전' },
    ]

    // 검색 타입이 변경될 때 입력값 초기화
    useEffect(() => {
        if (searchType.value === 'projectState') {
            setSearchValue('')
        } else {
            setProjectState({ value: '', label: '선택하세요' })
        }
    }, [searchType])

    // Alert 닫기 핸들러
    const handleAlertClose = () => {
        setShowAlert(false)
    }

    // 검색 유효성 검사
    const validateSearch = () => {
        if (isEmpty(searchType.value)) {
            setAlertMessage('검색 유형을 선택해주세요.');
            setShowAlert(true);
            return false;
        }

        if (searchType.value === 'projectState' && isEmpty(projectState.value)) {
            setAlertMessage('프로젝트 상태를 선택해주세요.');
            setShowAlert(true);
            return false;
        }

        if (searchType.value !== 'projectState' && isEmpty(searchValue.trim())) {
            setAlertMessage('검색어를 입력해주세요.');
            setShowAlert(true);
            return false;
        }

        return true;
    }

    // 검색 버튼 클릭 핸들러
    const handleSearch = () => {
        if (!validateSearch()) {
            return;
        }

        // 모든 조건이 충족되면 검색 실행
        onInputChange({
            type: searchType.value,
            value: searchType.value === 'projectState' ? projectState.value : searchValue.trim()
        });
    }

    // 초기화 버튼 클릭 핸들러
    const handleReset = () => {
        // 내부 상태 초기화
        setSearchValue('')
        setSearchType({ value: '', label: '선택하세요' })
        setProjectState({ value: '', label: '선택하세요' })
        setShowAlert(false)

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

    // 프로젝트 상태 변경 핸들러
    const handleProjectStateChange = (option) => {
        setProjectState(option)
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
                        placeholder="검색 유형"
                    />
                </div>

                {/* 검색 조건에 따른 입력 필드 */}
                {searchType.value === 'projectState' ? (
                    <div className="flex-1">
                        <Select
                            value={projectState}
                            options={projectStateOptions}
                            onChange={handleProjectStateChange}
                            isSearchable={false}
                            placeholder="프로젝트 상태"
                        />
                    </div>
                ) : (
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
                )}

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