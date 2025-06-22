import {useEffect, useState} from 'react'
import Card from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Notification from '@/components/ui/Notification'
import toast from '@/components/ui/toast'
import ConfirmDialog from '@/components/shared/ConfirmDialog'
import Input from '@/components/ui/Input'
import Select from '@/components/ui/Select'
import Upload from '@/components/ui/Upload'
import {HiOutlineArrowLeft, HiSave} from 'react-icons/hi'
import {FcImageFile} from 'react-icons/fc'
import {useNavigate} from 'react-router'
import {apiPrefix} from '@/configs/endpoint.config.js'
import axios from 'axios'
import Textarea from "@/views/ui-components/forms/Input/Textarea.jsx";

const DefectCreate = () => {
    const navigate = useNavigate()

    // 상태 변수들 선언
    const [saveDialogOpen, setSaveDialogOpen] = useState(false)
    const [alertDialogOpen, setAlertDialogOpen] = useState(false)
    const [alertMessage, setAlertMessage] = useState('')
    const [alertTitle, setAlertTitle] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [userOptions, setUserOptions] = useState([])
    const [projectOptions, setProjectOptions] = useState([''])
    const [uploadedFiles, setUploadedFiles] = useState([])

    // DefectRequestDto에 맞게 formData 구조 수정
    const [formData, setFormData] = useState({
        projectId: '',          // 프로젝트 ID
        assigneeId: '',         // 담당자 ID
        statusCode: 'DS1000',   // 상태 코드 (기본값: 결함등록)
        seriousCode: '',        // 심각도 코드
        orderCode: '',          // 순서 코드 (우선순위)
        defectDivCode: '',      // 결함 분류 코드
        defectTitle: '',        // 결함 제목
        defectMenuTitle: '',    // 결함 메뉴 제목
        defectUrlInfo: '',      // 결함 URL 정보
        defectContent: '',      // 결함 내용
        defectEtcContent: '',   // 기타 내용
        openYn: 'Y'             // 공개 여부 (기본값: 공개)
    })

    const defectSeriousOptions = [
        {value : '', label: '선택하세요'},
        {value : '5', label: '치명적'},
        {value : '4', label: '높음'},
        {value : '3', label: '보통'},
        {value : '2', label: '낮음'},
        {value : '1', label: '영향없음'},
    ]

    const priorityOptions = [
        {value : '', label: '선택하세요'},
        {value : 'MOMETLY', label: '즉시해결'},
        {value : 'WARNING', label: '주의요망'},
        {value : 'STANBY', label: '대기'},
        {value : 'IMPROVING', label: '개선권고'},
    ]

    const defectCategoryOptions = [
        { value: '', label: '선택하세요' },
        { value: 'SYSTEM', label: '시스템결함' },
        { value: 'FUNCTION', label: '기능결함' },
        { value: 'UI', label: 'UI결함' },
        { value: 'DOCUMENT', label: '문서결함' },
        { value: 'IMPROVING', label: '개선권고' },
        { value: 'NEW', label: '신규요청' },
    ]

    // 특정 프로젝트의 사용자 목록 가져오기
    const fetchProjectUsers = async (projectId) => {
        try {
            const response = await axios.get(`${apiPrefix}/projects/assignUserList`, {
                params: { projectId },
                withCredentials: true
            });

            // API 응답에서 userName과 userId를 사용하여 옵션 배열 생성
            const users = response.data.map(user => ({
                value: user.userId, // 선택 시 저장될 값
                label: user.userName // 화면에 표시될 이름
            }));

            setUserOptions(users);
        } catch (error) {
            console.error('사용자 목록을 가져오는 중 오류 발생:', error);
            toast.push(
                <Notification title={'데이터 로드 실패'} type="warning">
                    사용자 목록을 가져오는 중 오류가 발생했습니다.
                </Notification>
            );
        }
    };

    // 프로젝트 목록 가져오기
    useEffect(() => {
        const fetchDefectProjects = async () => {
            try {
                const response = await axios.get(`${apiPrefix}/defects/projectList`, {
                    withCredentials: true
                });

                // API 응답에서 사이트명과 프로젝트명을 조합하여 옵션 배열 생성
                const projects = response.data.map(project => ({
                    value: project.projectId, // 선택 시 저장될 값
                    label: project.projectName // 사이트명 / 프로젝트명 형태로 표시
                }));

                setProjectOptions(projects);
            } catch (error) {
                console.error('프로젝트 목록을 가져오는 중 오류 발생:', error);
                toast.push(
                    <Notification title={'데이터 로드 실패'} type="warning">
                        프로젝트 목록을 가져오는 중 오류가 발생했습니다.
                    </Notification>
                );
            }
        };

        fetchDefectProjects();
    }, []);

    // 파일 업로드 처리 함수 수정
    const handleFileUpload = (files) => {
        const newFiles = Array.from(files)
        console.log(newFiles)
        const totalFiles = newFiles.length
        console.log(totalFiles)

        if (totalFiles > 3) {
            // Alert 띄우기
            showAlert('업로드 제한', '최대 3개의 파일만 업로드할 수 있습니다.')
            return
        }

        setUploadedFiles(prev => [...prev, ...newFiles])
    }

    const handleFileRemove = (remainingFiles) => {
        setUploadedFiles(remainingFiles)
        console.log(remainingFiles.length)
    }

    const handleInputChange = (e) => {
        const { name, value } = e.target
        setFormData((prev) => ({
            ...prev,
            [name]: value,
        }))
    }

    // 중요도 선택 변경 처리
    const handleSeriousChange = (selectedOption) => {
        if (selectedOption) {
            setFormData((prev) => ({
                ...prev,
                seriousCode: selectedOption.value,
            }))
        }
    }

    // 우선순위(순서코드) 선택 변경 처리
    const handleOrderChange = (selectedOption) => {
        if (selectedOption) {
            setFormData((prev) => ({
                ...prev,
                orderCode: selectedOption.value,
            }))
        }
    }

    // 결함 분류 선택 변경 처리
    const handleCategoryChange = (selectedOption) => {
        if (selectedOption) {
            setFormData((prev) => ({
                ...prev,
                defectDivCode: selectedOption.value,
            }))
        }
    }

    // 프로젝트 선택 변경 처리
    const handleProjectChange = (selectedOption) => {
        if (selectedOption && selectedOption.value) {
            setFormData((prev) => ({
                ...prev,
                projectId: selectedOption.value,
                assigneeId: '' // 프로젝트 변경 시 담당자 초기화
            }));

            // 선택된 프로젝트의 사용자 목록 가져오기
            fetchProjectUsers(selectedOption.value);
        } else {
            // 프로젝트 선택 해제 시
            setFormData((prev) => ({
                ...prev,
                projectId: '',
                assigneeId: ''
            }));
            setUserOptions([]); // 사용자 목록 초기화
        }
    }

    // 담당자 선택 변경 처리
    const handleAssigneeChange = (selectedOption) => {
        if (selectedOption) {
            setFormData((prev) => ({
                ...prev,
                assigneeId: selectedOption.value,
            }))
        }
    }

    const handleBackToList = () => {
        navigate('/defect-management') // 결함관리 페이지로 이동하도록 수정
    }

    // 경고창 닫기
    const handleAlertClose = () => {
        setAlertDialogOpen(false)
    }

    // 경고창 표시 함수
    const showAlert = (title, message) => {
        setAlertTitle(title)
        setAlertMessage(message)
        setAlertDialogOpen(true)
    }

    // 저장 다이얼로그 관련 함수
    const handleSaveDialogClose = () => {
        setSaveDialogOpen(false)
    }

    const handleSaveDialogOpen = (e) => {
        e.preventDefault() // 폼 제출 방지

        // 필수 필드 검증
        if (!formData.projectId) {
            showAlert('프로젝트 미선택', '프로젝트를 선택해주세요.')
            return
        }

        if (!formData.assigneeId) {
            showAlert('담당자 미선택', '담당자를 선택해주세요.')
            return
        }

        if (!formData.defectTitle) {
            showAlert('결함 제목 미입력', '결함 제목을 입력해주세요.')
            return
        }

        if (!formData.defectContent) {
            showAlert('결함 상세 미입력', '결함 상세 내용을 입력해주세요.')
            return
        }

        // 모든 검증을 통과하면 저장 다이얼로그 열기
        setSaveDialogOpen(true)
    }

    // 실제 저장 처리 함수
    const handleSave = async () => {
        try {
            setIsSubmitting(true)

            // FormData 객체 생성 (파일 업로드를 위해)
            const formDataToSend = new FormData()

            // DefectRequestDto 구조에 맞춰 요청 데이터 구성
            const requestData = {
                projectId: formData.projectId,
                assigneeId: formData.assigneeId,
                statusCode: formData.statusCode,
                seriousCode: formData.seriousCode,
                orderCode: formData.orderCode,
                defectDivCode: formData.defectDivCode,
                defectTitle: formData.defectTitle,
                defectMenuTitle: formData.defectMenuTitle,
                defectUrlInfo: formData.defectUrlInfo,
                defectContent: formData.defectContent,
                defectEtcContent: formData.defectEtcContent,
                openYn: formData.openYn
            }

            // JSON 데이터를 Blob으로 추가
            formDataToSend.append('defectRequestDto', new Blob([JSON.stringify(requestData)], {
                type: 'application/json'
            }))

            // 파일들을 FormData에 추가
            if (uploadedFiles && uploadedFiles.length > 0) {
                uploadedFiles.forEach((file) => {
                    formDataToSend.append('files', file)
                })
            }

            // 서버에 결함 등록 요청
            await axios.post(
                `${apiPrefix}/defects/save`,
                formDataToSend,
                {
                    headers: {
                        'Content-Type': 'multipart/form-data',
                    },
                    withCredentials: true,
                },
            )

            toast.push(
                <Notification title={'등록 성공'} type="success">
                    결함이 성공적으로 등록되었습니다
                </Notification>,
            )

            // 결함 관리 페이지로 이동
            navigate('/defect-management')
        } catch (error) {
            toast.push(
                <Notification title={'등록 실패'} type="warning">
                    {error.response?.data?.error ||
                        '처리중 오류가 발생되었습니다.'}
                </Notification>,
            )

            return false
        } finally {
            setIsSubmitting(false)
            setSaveDialogOpen(false)
        }
    }

    return (
        <Card className="w-full">
            <form onSubmit={handleSaveDialogOpen}>
                <div className="flex justify-between items-center mb-4">
                    <h4 className="font-bold">결함 신규등록</h4>
                    <div className="flex gap-2">
                        <Button
                            type="button"
                            icon={<HiOutlineArrowLeft />}
                            onClick={handleBackToList}
                        >
                            취소
                        </Button>
                        <Button
                            type="submit"
                            variant="solid"
                            icon={<HiSave />}
                            loading={isSubmitting}
                        >
                            등록
                        </Button>
                    </div>
                </div>

                <div className="flex flex-col xl:justify-between h-full 2xl:min-w-[360px] mx-auto">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-7 mt-10">
                        <div>
                            <label className="font-semibold block mb-2">사이트 / 프로젝트명</label>
                            <Select
                                options={[{value: '', label: '선택하세요'}, ...projectOptions]}
                                value={[{value: '', label: '선택하세요'}, ...projectOptions].find(option => option.value === formData.projectId) || null}
                                onChange={handleProjectChange}
                                placeholder="프로젝트 선택"
                                isSearchable={false}
                                openMenuOnFocus={true}
                                isClearable={false}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">담당자</label>
                            <Select
                                options={formData.projectId ? [{value: '', label: '선택하세요'}, ...userOptions] : [{value: '', label: '프로젝트를 먼저 선택하세요'}]}
                                value={formData.projectId ? [{value: '', label: '선택하세요'}, ...userOptions].find(option => option.value === formData.assigneeId) || null : null}
                                onChange={handleAssigneeChange}
                                placeholder="담당자 선택"
                                isSearchable={false}
                                openMenuOnFocus={true}
                                isClearable={false}
                                isDisabled={!formData.projectId}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">중요도</label>
                            <Select
                                options={defectSeriousOptions}
                                value={defectSeriousOptions.find(option => option.value === formData.seriousCode) || null}
                                onChange={handleSeriousChange}
                                placeholder="중요도 선택"
                                isSearchable={false}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">우선순위</label>
                            <Select
                                options={priorityOptions}
                                value={priorityOptions.find(option => option.value === formData.orderCode) || null}
                                onChange={handleOrderChange}
                                placeholder="우선순위 선택"
                                isSearchable={false}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">결함 분류</label>
                            <Select
                                options={defectCategoryOptions}
                                value={defectCategoryOptions.find(option => option.value === formData.defectDivCode) || null}
                                onChange={handleCategoryChange}
                                placeholder="결함 분류 선택"
                                isSearchable={false}
                            />
                        </div>

                        <div className="md:col-span-2">
                            <label className="font-semibold block mb-2">결함 요약(제목)</label>
                            <Input
                                type="text"
                                name="defectTitle"
                                value={formData.defectTitle}
                                onChange={handleInputChange}
                                placeholder="결함 요약(제목)"
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">결함 발생 메뉴</label>
                            <Input
                                type="text"
                                name="defectMenuTitle"
                                value={formData.defectMenuTitle}
                                onChange={handleInputChange}
                                placeholder="결함 발생 메뉴"
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">결함 발생 URL</label>
                            <Input
                                type="text"
                                name="defectUrlInfo"
                                value={formData.defectUrlInfo}
                                onChange={handleInputChange}
                                placeholder="결함 발생 URL"
                            />
                        </div>

                        <div className="md:col-span-2">
                            <label className="font-semibold block mb-2">결함 상세(설명)</label>
                            <Textarea
                                name="defectContent"
                                value={formData.defectContent}
                                onChange={handleInputChange}
                                placeholder="결함상세 설명 입력"
                            />
                        </div>

                        {/* 파일 업로드 섹션 */}
                        <div className="md:col-span-2">
                            <div className="flex items-center justify-between mb-2">
                                <label className="font-semibold">첨부 파일 (최대 3개)</label>
                            </div>
                            <div className="space-y-4">
                                <div>
                                    <Upload
                                        draggable
                                        multiple
                                        onChange={handleFileUpload}
                                        onFileRemove={handleFileRemove}
                                        uploadLimit={3}
                                        accept=".jpeg,.jpg,.png,.gif,.pdf,.doc,.docx"
                                    >
                                        <div className="my-16 text-center">
                                            <div className="text-6xl mb-4 flex justify-center">
                                                <FcImageFile />
                                            </div>
                                            <p className="font-semibold">
                                                <span className="text-gray-800 dark:text-white">
                                                    파일을 여기에 드롭하거나{' '}
                                                </span>
                                                <span className="text-blue-500">찾아보기</span>
                                            </p>
                                            <p className="mt-1 opacity-60 dark:text-white">
                                                지원 형식: jpeg, png, gif, pdf, doc, docx
                                            </p>
                                        </div>
                                    </Upload>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* 저장 확인 다이얼로그 */}
                <ConfirmDialog
                    isOpen={saveDialogOpen}
                    title="결함 등록"
                    onClose={handleSaveDialogClose}
                    onRequestClose={handleSaveDialogClose}
                    onCancel={handleSaveDialogClose}
                    onConfirm={handleSave}
                    confirmText={'등록'}
                >
                    <p>결함을 등록하시겠습니까?</p>
                </ConfirmDialog>

                {/* 경고 다이얼로그 - 알림 형태로 취소 버튼만 노출 */}
                <ConfirmDialog
                    type="warning"
                    isOpen={alertDialogOpen}
                    title={alertTitle}
                    onClose={handleAlertClose}
                    onRequestClose={handleAlertClose}
                    onCancel={handleAlertClose}
                    onConfirm={handleAlertClose}
                    confirmText={'확인'}
                    cancelButtonProps={{ style: { display: 'none' } }}
                >
                    <p>{alertMessage}</p>
                </ConfirmDialog>
            </form>
        </Card>
    )
}

export default DefectCreate