import { useEffect, useState } from 'react'
import Card from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Notification from '@/components/ui/Notification'
import toast from '@/components/ui/toast'
import ConfirmDialog from '@/components/shared/ConfirmDialog'
import Input from '@/components/ui/Input'
import Select from '@/components/ui/Select'
import Upload from '@/components/ui/Upload'
import { HiOutlineArrowLeft, HiSave, HiDownload, HiTrash } from 'react-icons/hi'
import { FcImageFile } from 'react-icons/fc'
import { useNavigate, useParams } from 'react-router'
import { apiPrefix } from '@/configs/endpoint.config.js'
import axios from 'axios'
import useSWR from 'swr'
import Textarea from "@/views/ui-components/forms/Input/Textarea.jsx"

const DefectEdit = () => {
    const { defectId } = useParams()
    const navigate = useNavigate()

    // 상태 변수들 선언
    const [saveDialogOpen, setSaveDialogOpen] = useState(false)
    const [alertDialogOpen, setAlertDialogOpen] = useState(false)
    const [alertMessage, setAlertMessage] = useState('')
    const [alertTitle, setAlertTitle] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [userOptions, setUserOptions] = useState([])
    const [projectOptions, setProjectOptions] = useState([])
    const [uploadedFiles, setUploadedFiles] = useState([])
    const [existingFiles, setExistingFiles] = useState([])
    
    // DefectRequestDto에 맞게 formData 구조 수정
    const [formData, setFormData] = useState({
        defectId: '',           // 결함 ID (수정용)
        projectId: '',          // 프로젝트 ID
        assigneeId: '',         // 담당자 ID
        statusCode: '',         // 상태 코드
        seriousCode: '',        // 심각도 코드
        orderCode: '',          // 순서 코드 (우선순위)
        defectDivCode: '',      // 결함 분류 코드
        defectTitle: '',        // 결함 제목
        defectMenuTitle: '',    // 결함 메뉴 제목
        defectUrlInfo: '',      // 결함 URL 정보
        defectContent: '',      // 결함 내용
        defectEtcContent: '',   // 기타 내용
        openYn: 'Y'             // 공개 여부
    })

    const defectSeriousOptions = [
        {value : '', label: '선택하세요'},
        {value : '1', label: '영향없음'},
        {value : '2', label: '낮음'},
        {value : '3', label: '보통'},
        {value : '4', label: '높음'},
        {value : '5', label: '치명적'},
    ]

    const priorityOptions = [
        {value : '', label: '선택하세요'},
        {value : '1', label: '낮음'},
        {value : '2', label: '보통'},
        {value : '3', label: '높음'},
        {value : '4', label: '긴급'},
        {value : '5', label: '최우선'},
    ]

    const defectCategoryOptions = [
        {value : '', label: '선택하세요'},
        {value : 'DS1000', label: '결함등록'},
        {value : 'DS2000', label: '결함할당'},
        {value : 'DS3000', label: '결함조치 완료'},
        {value : 'DS3005', label: 'To-Do처리'},
        {value : 'DS3006', label: 'To-Do(조치대기)'},
        {value : 'DS4000', label: '결함조치 보류(결함아님)'},
        {value : 'DS4001', label: '결함조치 반려(조치안됨)'},
        {value : 'DS4002', label: '결함 재발생'},
        {value : 'DS5000', label: '결함종료'},
        {value : 'DS6000', label: '결함해제'},
    ]

    // 결함 정보 로드 - 컨트롤러 API 엔드포인트에 맞게 수정
    const { data, isLoading, error } = useSWR(
        defectId ? `/defects/read/${defectId}` : null,
        (url) => axios.get(`${apiPrefix}${url}`, { 
            withCredentials: true 
        }).then(res => res.data),
        {
            revalidateOnFocus: false,
            revalidateIfStale: false,
            revalidateOnMount: true,
        },
    )

    // 할당 가능한 사용자 목록 가져오기
    useEffect(() => {
        const fetchUsers = async () => {
            try {
                const response = await axios.get(`${apiPrefix}/projects/assignUserList`, {
                    withCredentials: true
                });

                const users = response.data.map(user => ({
                    value: user.userId,
                    label: user.userName
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

        const fetchDefectProjects = async () => {
            try {
                const response = await axios.get(`${apiPrefix}/defects/projectList`, {
                    withCredentials: true
                });

                const projects = response.data.map(project => ({
                    value: project.projectId,
                    label: project.projectName
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

        fetchUsers();
        fetchDefectProjects();
    }, []);

    // 데이터가 로드되면 폼 데이터 설정
    useEffect(() => {
        if (data) {
            setFormData({
                defectId: data.defectId || '',
                projectId: data.projectId || '',
                assigneeId: data.assigneeId || '',
                statusCode: data.statusCode || '',
                seriousCode: data.seriousCode || '',
                orderCode: data.orderCode || '',
                defectDivCode: data.defectDivCode || '',
                defectTitle: data.defectTitle || '',
                defectMenuTitle: data.defectMenuTitle || '',
                defectUrlInfo: data.defectUrlInfo || '',
                defectContent: data.defectContent || '',
                defectEtcContent: data.defectEtcContent || '',
                openYn: data.openYn || 'Y'
            });

            // 기존 첨부파일이 있다면 설정
            if (data.attachmentFiles) {
                setExistingFiles(data.attachmentFiles);
            }
        }
    }, [data]);

    if (isLoading) {
        return (
            <div className="w-full p-5">
                <div className="text-center">데이터를 불러오는 중입니다...</div>
            </div>
        )
    }

    if (error) {
        return (
            <div className="w-full p-5">
                <div className="text-center text-red-500">
                    결함 정보를 불러오는 중 오류가 발생했습니다. 다시 시도해 주세요.
                </div>
            </div>
        )
    }

    // 파일 다운로드 함수
    // 파일 다운로드 함수
    const handleFileDownload = async (file) => {
        try {
            const fileName = file.sysFileName

            if (!fileName) {
                toast.push(
                    <Notification title={'다운로드 실패'} type="warning">
                        다운로드에 사용할 파일 이름을 찾을 수 없습니다.
                    </Notification>
                );
                return;
            }


            const response = await axios.get(
                `${apiPrefix}/files/download/${encodeURIComponent(fileName)}`,
                {
                    responseType: 'blob',
                    withCredentials: true
                }
            );

            // 기본 파일명 설정 - 여러 속성 중 존재하는 것 사용
            let downloadFileName = file.sysFileName;

            // 파일 확장자가 없는 경우 원본 파일명에서 추출하여 추가
            if (downloadFileName && !downloadFileName.includes('.') && file.sysFileName) {
                const sysFileNameParts = file.sysFileName.split('.');
                if (sysFileNameParts.length > 1) {
                    const extension = sysFileNameParts[sysFileNameParts.length - 1];
                    downloadFileName = `${downloadFileName}.${extension}`;
                }
            }

            // Content-Disposition 헤더에서 파일명 추출
            const contentDisposition = response.headers['content-disposition'];
            if (contentDisposition) {

                // UTF-8 인코딩된 파일명 처리
                const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/);
                if (utf8Match) {
                    try {
                        downloadFileName = decodeURIComponent(utf8Match[1]);
                    } catch (e) {
                        console.warn('UTF-8 파일명 디코딩 실패:', e);
                    }
                } else {
                    // 일반 filename 속성 처리
                    const match = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                    if (match) {
                        downloadFileName = match[1].replace(/['"]/g, '');
                        try {
                            downloadFileName = decodeURIComponent(downloadFileName);
                        } catch (e) {
                            console.warn('파일명 디코딩 실패:', e);
                        }
                    }
                }
            }

            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', downloadFileName);
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            toast.push(
                <Notification title={'다운로드 완료'} type="success">
                    파일이 성공적으로 다운로드되었습니다.
                </Notification>
            );
        } catch (error) {
            console.error('파일 다운로드 오류:', error);
            let errorMessage = '파일 다운로드 중 오류가 발생했습니다.';

            if (!error.response) {
                errorMessage = '서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요.';
            } else if (error.response.status === 404) {
                errorMessage = '파일을 찾을 수 없습니다.';
            } else if (error.response.status === 500) {
                errorMessage = '서버에서 파일을 처리하는 중 오류가 발생했습니다.';
            }

            toast.push(
                <Notification title={'다운로드 실패'} type="warning">
                    {errorMessage}
                </Notification>
            );
        }
    };




    // 파일 업로드 처리 함수
    const handleFileUpload = (files) => {
        const newFiles = Array.from(files)
        const totalFiles = uploadedFiles.length + newFiles.length + existingFiles.length

        if (totalFiles > 3) {
            showAlert('업로드 제한', '최대 3개의 파일만 업로드할 수 있습니다.')
            return
        }

        setUploadedFiles(prev => [...prev, ...newFiles])
    }

    const handleFileRemove = (remainingFiles) => {
        setUploadedFiles(remainingFiles)
    }

    // 기존 파일 삭제 처리
    const handleExistingFileRemove = (fileId) => {
        setExistingFiles(prev => prev.filter(file => file.fileId !== fileId))
    }

    const handleInputChange = (e) => {
        const { name, value } = e.target
        setFormData((prev) => ({
            ...prev,
            [name]: value,
        }))
    }

    // 심각도 선택 변경 처리
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
        if (selectedOption) {
            setFormData((prev) => ({
                ...prev,
                projectId: selectedOption.value,
            }))
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
        navigate('/defect-management')
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
                defectId: formData.defectId,
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
                openYn: formData.openYn,
                // 삭제된 기존 파일 ID 목록 (원본 데이터와 현재 상태 비교)
                deletedFileIds: data.attachmentFiles 
                    ? data.attachmentFiles
                        .filter(originalFile => !existingFiles.some(currentFile => currentFile.fileId === originalFile.fileId))
                        .map(file => file.fileId)
                    : []
            }

            // JSON 데이터를 Blob으로 추가
            formDataToSend.append('defectRequestDto', new Blob([JSON.stringify(requestData)], {
                type: 'application/json'
            }))

            // 새로 추가된 파일들을 FormData에 추가
            if (uploadedFiles && uploadedFiles.length > 0) {
                uploadedFiles.forEach((file) => {
                    formDataToSend.append('files', file)
                })
            }

            // 서버에 결함 수정 요청
            await axios.put(
                `${apiPrefix}/defects/modify-defects`,
                formDataToSend,
                {
                    headers: {
                        'Content-Type': 'multipart/form-data',
                    },
                    withCredentials: true,
                },
            )

            toast.push(
                <Notification title={'수정 성공'} type="success">
                    결함이 성공적으로 수정되었습니다
                </Notification>,
            )

            // 결함 관리 페이지로 이동
            navigate('/defect-management')
        } catch (error) {
            toast.push(
                <Notification title={'수정 실패'} type="warning">
                    {error.response?.data?.error ||
                        '처리중 오류가 발생되었습니다.'}
                </Notification>,
            )
        } finally {
            setIsSubmitting(false)
            setSaveDialogOpen(false)
        }
    }

    return (
        <Card className="w-full">
            <form onSubmit={handleSaveDialogOpen}>
                <div className="flex justify-between items-center mb-4">
                    <h4 className="font-bold">결함 수정</h4>
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
                            수정
                        </Button>
                    </div>
                </div>

                <div className="flex flex-col xl:justify-between h-full 2xl:min-w-[360px] mx-auto">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-7 mt-10">
                        <div>
                            <label className="font-semibold block mb-2">사이트 / 프로젝트명</label>
                            <Select
                                options={projectOptions}
                                value={projectOptions.find(option => option.value === formData.projectId) || null}
                                onChange={handleProjectChange}
                                placeholder="프로젝트 선택"
                                isSearchable={true}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">담당자</label>
                            <Select
                                options={userOptions}
                                value={userOptions.find(option => option.value === formData.assigneeId) || null}
                                onChange={handleAssigneeChange}
                                placeholder="담당자 선택"
                                isSearchable={true}
                            />
                        </div>

                        <div>
                            <label className="font-semibold block mb-2">심각도</label>
                            <Select
                                options={defectSeriousOptions}
                                value={defectSeriousOptions.find(option => option.value === formData.seriousCode) || null}
                                onChange={handleSeriousChange}
                                placeholder="심각도 선택"
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

                        {/* 기존 파일 목록 */}
                        {existingFiles.length > 0 && (
                            <div className="md:col-span-2">
                                <label className="font-semibold block mb-2">기존 첨부 파일</label>
                                <div className="space-y-2">
                                    {existingFiles.map((file) => (
                                        <div key={file.fileId} className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700 rounded-lg">
                                            <div className="flex items-center space-x-3">
                                                <FcImageFile className="text-lg" />
                                                <div>
                                                    <p className="text-sm font-medium text-gray-900 dark:text-white">
                                                        {file.orgFileName}
                                                    </p>
                                                    {file.fileSize && (
                                                        <p className="text-xs text-gray-500 dark:text-gray-400">
                                                            {Math.round(file.fileSize / 1024)} KB
                                                        </p>
                                                    )}
                                                </div>
                                            </div>
                                            <div className="flex items-center space-x-2">
                                                <Button
                                                    type="button"
                                                    size="xs"
                                                    variant="solid"
                                                    color="blue-600"
                                                    icon={<HiDownload />}
                                                    onClick={() => handleFileDownload(file)}
                                                >
                                                    다운로드
                                                </Button>
                                                <Button
                                                    type="button"
                                                    size="xs"
                                                    variant="solid"
                                                    color="red-600"
                                                    icon={<HiTrash />}
                                                    onClick={() => handleExistingFileRemove(file.fileId)}
                                                >
                                                    삭제
                                                </Button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* 파일 업로드 섹션 */}
                        <div className="md:col-span-2">
                            <div className="flex items-center justify-between mb-2">
                                <label className="font-semibold">
                                    새 파일 업로드 (최대 3개, 현재: {existingFiles.length + uploadedFiles.length}개)
                                </label>
                            </div>
                            <div className="space-y-4">
                                <div>
                                    <Upload
                                        draggable
                                        multiple
                                        onChange={handleFileUpload}
                                        onFileRemove={handleFileRemove}
                                        uploadLimit={3 - existingFiles.length}
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
                    title="결함 수정"
                    onClose={handleSaveDialogClose}
                    onRequestClose={handleSaveDialogClose}
                    onCancel={handleSaveDialogClose}
                    onConfirm={handleSave}
                    confirmText={'수정'}
                >
                    <p>결함을 수정하시겠습니까?</p>
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

export default DefectEdit