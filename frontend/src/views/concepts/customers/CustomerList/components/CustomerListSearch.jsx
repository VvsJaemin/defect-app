import { TbSearch, TbRefresh } from 'react-icons/tb'
import { Button } from '@/components/ui/Button'
import { useState, useEffect } from 'react'
import Input from '@/components/ui/Input/Input.jsx'
import Select from '@/components/ui/Select/Select.jsx'
import isEmpty from 'lodash/isEmpty'
import Alert from '@/components/ui/Alert/Alert.jsx'

const CustomerListSearch = (props) => {
    const { onInputChange, onReset, ref } = props
    const [searchValue, setSearchValue] = useState('')
    const [searchType, setSearchType] = useState({ value: '', label: '선택하세요' }) // 기본 검색 타입
    const [userRoleValue, setUserRoleValue] = useState({ value: '', label: '권한 선택' }) // 사용자 권한 값
    const [alertMessage, setAlertMessage] = useState('')
    const [showAlert, setShowAlert] = useState(false)

    // 검색 타입 옵션
    const searchOptions = [
        { value: '', label: '선택하세요' },
        { value: 'userName', label: '사용자명' },
        { value: 'userId', label: '계정' },
        { value: 'userSeCd', label: '사용자 권한' }
    ]

    // 사용자 권한 옵션 (예시 데이터, 실제 데이터로 교체 필요)
    const userRoleOptions = [
        { value: '', label: '권한 선택' },
        { value: 'MG', label: '처리현황 조회(manager)' },
        { value: 'QA', label: '결함등록/완료(Q/A)' },
        { value: 'CU', label: '고객사' },
        { value: 'DM', label: '결함검토/할당(dev manager)' },
        { value: 'DP', label: '결함처리(developer)' },
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

        // 사용자 권한 검색 시 권한이 선택되지 않은 경우
        if (searchType.value === 'userSeCd' && isEmpty(userRoleValue.value)) {
            setAlertMessage('사용자 권한을 선택해주세요.');
            setShowAlert(true);
            return;
        }

        // 일반 검색어 입력 시 검색어가 비어있는 경우 (권한 검색은 제외)
        if (searchType.value !== 'userSeCd' && isEmpty(searchValue)) {
            setAlertMessage('검색어를 입력해주세요.');
            setShowAlert(true);
            return;
        }

        // 모든 조건이 충족되면 검색 실행
        onInputChange({
            type: searchType.value,
            value: searchType.value === 'userSeCd' ? userRoleValue.value : searchValue
        });
    }

    // 초기화 버튼 클릭 핸들러
    const handleReset = () => {
        // 내부 상태 초기화
        setSearchValue('')
        setSearchType({ value: '', label: '선택하세요' })
        setUserRoleValue({ value: '', label: '권한 선택' })

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
        
        // 검색 타입이 변경되면 검색어 초기화
        if (option.value !== 'userSeCd') {
            setUserRoleValue({ value: '', label: '권한 선택' })
        }
        if (option.value === 'userSeCd') {
            setSearchValue('')
        }
    }

    // 사용자 권한 변경 핸들러
    const handleUserRoleChange = (option) => {
        setUserRoleValue(option)
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

                {/* 검색어 입력 또는 사용자 권한 선택 */}
                <div className="flex-1">
                    {searchType.value === 'userSeCd' ? (
                        <Select
                            value={userRoleValue}
                            options={userRoleOptions}
                            onChange={handleUserRoleChange}
                            isSearchable={false}
                        />
                    ) : (
                        <Input
                            ref={ref}
                            placeholder="검색어를 입력하세요."
                            suffix={<TbSearch className="text-lg w-6 h-6" />}
                            onChange={handleInputChange}
                            value={searchValue}
                            onKeyPress={handleKeyPress}
                        />
                    )}
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

export default CustomerListSearch