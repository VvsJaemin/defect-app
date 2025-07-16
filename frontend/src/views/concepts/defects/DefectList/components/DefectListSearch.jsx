import { TbRefresh, TbSearch } from 'react-icons/tb'
import { FaCircle } from 'react-icons/fa'
import { Button } from '@/components/ui/Button'
import { useEffect, useState } from 'react'
import Input from '@/components/ui/Input/Input.jsx'
import Select from '@/components/ui/Select/Select.jsx'
import isEmpty from 'lodash/isEmpty'
import Alert from '@/components/ui/Alert/Alert.jsx'
import { useLocation } from 'react-router'
import ApiService from '@/services/ApiService.js'

const DefectListSearch = (props) => {
    const { onInputChange, onReset, ref } = props
    const [searchValue, setSearchValue] = useState('')
    const [searchType, setSearchType] = useState({
        value: '',
        label: '선택하세요',
    })
    const [defectState, setDefectState] = useState({ value: '', label: '전체' })
    const [projectName, setProjectName] = useState({ value: '', label: '전체' })
    const [assignee, setAssignee] = useState({ value: '', label: '전체' })
    const [alertMessage, setAlertMessage] = useState('')
    const [showAlert, setShowAlert] = useState(false)
    const [projectOptions, setProjectOptions] = useState([])
    const [userOptions, setUserOptions] = useState([])

    const location = useLocation()

    // 내게할당된 결함 페이지인지 확인
    const isAssignedPage = location.pathname === '/defect-management/assigned'

    // 검색 타입 옵션 (결함상태 제거)
    const searchOptions = [
        { value: '', label: '선택하세요' },
        { value: 'defectId', label: '결함아이디' },
        { value: 'defectTitle', label: '결함요약' },
    ]

    // 결함상태 옵션
    const defectStateOptions = [
        { value: '', label: '전체' },
        { value: 'DS1000', label: '결함등록' },
        { value: 'DS2000', label: '결함할당' },
        { value: 'DS3000', label: '결함조치 완료' },
        { value: 'DS4000', label: '결함조치 보류(결함아님)' },
        { value: 'DS4001', label: '결함조치 반려(조치안됨)' },
        { value: 'DS4002', label: '결함 재발생' },
        { value: 'DS3005', label: 'To-Do처리' },
        { value: 'DS3006', label: 'To-Do(조치대기)' },
    ]

    // 전체 사용자 목록 가져오기
    const fetchUsers = async () => {
        try {
            const response = await ApiService.get(
                '/projects/assignUserList',
                {},
            )

            // API 응답에서 userName과 userId를 사용하여 옵션 배열 생성
            const users = response.data.map((user) => ({
                value: user.userId, // 선택 시 저장될 값
                label: user.userName, // 화면에 표시될 이름
            }))

            setUserOptions([{ value: '', label: '전체' }, ...users])
        } catch (error) {
            console.error('사용자 목록을 가져오는 중 오류 발생:', error)
        }
    }

    // 프로젝트 목록 가져오기
    useEffect(() => {
        const fetchDefectProjects = async () => {
            try {
                const response = await ApiService.get(
                    '/defects/projectList',
                    {},
                )

                // API 응답에서 사이트명과 프로젝트명을 조합하여 옵션 배열 생성
                const projects = response.data.map((project) => ({
                    value: project.projectId, // 선택 시 저장될 값
                    label: project.projectName, // 사이트명 / 프로젝트명 형태로 표시
                }))

                setProjectOptions([{ value: '', label: '전체' }, ...projects])
            } catch (error) {
                console.error('프로젝트 목록을 가져오는 중 오류 발생:', error)
            }
        }

        fetchDefectProjects()

        // 내개할당된 결함 페이지가 아닐 때만 사용자 목록 가져오기
        if (!isAssignedPage) {
            fetchUsers()
        }
    }, [isAssignedPage])

    // 검색 타입이 변경될 때 입력값 초기화
    useEffect(() => {
        setSearchValue('')
    }, [searchType])

    // Alert 닫기 핸들러
    const handleAlertClose = () => {
        setShowAlert(false)
    }

    // 검색 유효성 검사
    const validateSearch = () => {
        // 검색 타입이 선택되었지만 검색어가 없는 경우에만 오류 표시
        if (!isEmpty(searchType.value) && isEmpty(searchValue.trim())) {
            setAlertMessage('검색어를 입력해주세요.')
            setShowAlert(true)
            return false
        }

        return true
    }

    // 검색 버튼 클릭 핸들러 수정
    const handleSearch = () => {
        if (!validateSearch()) {
            return
        }

        // 검색 조건을 객체로 전달
        const searchParams = {}

        if (!isEmpty(searchType.value) && !isEmpty(searchValue.trim())) {
            searchParams[searchType.value] = searchValue.trim()
        }

        if (defectState.value && defectState.value !== '') {
            searchParams.statusCode = defectState.value
        }

        if (projectName.value && projectName.value !== '') {
            searchParams.projectId = projectName.value
        }

        // 내개할당된 결함 페이지가 아닐 때만 담당자 조건 추가
        if (!isAssignedPage && assignee.value && assignee.value !== '') {
            searchParams.assigneeId = assignee.value
        }

        onInputChange(searchParams)
    }

    // 초기화 버튼 클릭 핸들러
    const handleReset = () => {
        // 내부 상태 초기화
        setSearchValue('')
        setSearchType({ value: '', label: '선택하세요' })
        setDefectState({ value: '', label: '전체' })
        setProjectName({ value: '', label: '전체' })
        if (!isAssignedPage) {
            setAssignee({ value: '', label: '전체' })
        }
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

    // 결함 상태 변경 핸들러
    const handleDefectStateChange = (option) => {
        setDefectState(option)
    }

    // 프로젝트명 변경 핸들러
    const handleProjectNameChange = (option) => {
        setProjectName(option)
    }

    // 담당자 변경 핸들러
    const handleAssigneeChange = (option) => {
        setAssignee(option)
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

            <div className="flex gap-2 mt-3 flex-wrap">
                {/* 프로젝트명 선택 (별도 검색조건) */}
                <div className="w-72 flex items-center gap-2">
                    <label className="text-sm font-bold text-gray-700 whitespace-nowrap">
                        <FaCircle
                            className="inline text-yellow-500 text-xs mr-1"
                            aria-hidden="true"
                        />
                        프로젝트
                    </label>
                    <div className="flex-1">
                        <Select
                            value={projectName}
                            options={projectOptions}
                            onChange={handleProjectNameChange}
                            isSearchable={false}
                            placeholder="프로젝트 선택"
                        />
                    </div>
                </div>

                {/* 담당자 선택 (내개할당된 결함 페이지에서는 숨김) */}
                {!isAssignedPage && (
                    <div className="w-56 flex items-center gap-2">
                        <label className="text-sm font-bold text-gray-700 whitespace-nowrap">
                            <FaCircle
                                className="inline text-yellow-500 text-xs mr-1"
                                aria-hidden="true"
                            />
                            담당자
                        </label>
                        <div className="flex-1">
                            <Select
                                value={assignee}
                                options={userOptions}
                                onChange={handleAssigneeChange}
                                isSearchable={false}
                                placeholder="담당자 선택"
                            />
                        </div>
                    </div>
                )}

                {/* 결함상태 선택 (별도 검색조건) */}
                <div className="w-72 flex items-center gap-2">
                    <label className="text-sm font-bold text-gray-700 whitespace-nowrap">
                        <FaCircle
                            className="inline text-yellow-500 text-xs mr-1"
                            aria-hidden="true"
                        />
                        결함상태
                    </label>
                    <div className="flex-1">
                        <Select
                            value={defectState}
                            options={defectStateOptions}
                            onChange={handleDefectStateChange}
                            isSearchable={false}
                            placeholder="결함 상태"
                        />
                    </div>
                </div>

                {/* 검색어 영역 */}
                <div className="flex items-center gap-2">
                    <label className="text-sm font-bold text-gray-700 whitespace-nowrap">
                        <FaCircle
                            className="inline text-yellow-500 text-xs mr-1"
                            aria-hidden="true"
                        />
                        검색어
                    </label>

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

                    {/* 검색어 입력 필드 */}
                    <div className="w-50">
                        <Input
                            ref={ref}
                            placeholder="검색어를 입력하세요."
                            suffix={<TbSearch className="text-lg" />}
                            onChange={handleInputChange}
                            value={searchValue}
                            onKeyPress={handleKeyPress}
                        />
                    </div>
                </div>

                {/* 검색 버튼 */}
                <Button
                    variant="solid"
                    onClick={handleSearch}
                    icon={<TbSearch className="text-lg" />}
                >
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

export default DefectListSearch