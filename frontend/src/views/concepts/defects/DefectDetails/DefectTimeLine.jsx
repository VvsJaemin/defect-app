import Card from '@/components/ui/Card'
import Timeline from '@/components/ui/Timeline'
import dayjs from 'dayjs'
import { HiOutlineDocumentText, HiOutlineExternalLink } from 'react-icons/hi'
import axios from 'axios'
import { apiPrefix } from '@/configs/endpoint.config.js'

const DefectTimeline = ({
    data,
    logCt,
    uploadedFile,
    onLogCtChange,
    onFileChange,
}) => {
    const formatDate = (dateString) => {
        if (!dateString) return '-'
        return dayjs(dateString).format('YYYY-MM-DD hh:mm A')
    }

    // 쿼리 결과에 맞춰 defectLogs를 사용
    const defectLogs = data.content

    console.log(defectLogs)

    // 담당자 정보는 첫 번째 로그에서 가져오거나 별도로 전달받은 데이터에서 가져옴
    const assigneeInfo = defectLogs.length > 0 ? defectLogs[0] : null

    const getStatusBadge = (statusCode) => {
        const statusConfig = {
            결함등록: 'bg-blue-100 text-blue-800',
            처리중: 'bg-yellow-100 text-yellow-800',
            '결함조치 완료': 'bg-green-100 text-green-800',
            보류: 'bg-gray-100 text-gray-800',
            이관: 'bg-purple-100 text-purple-800',
        }

        return statusConfig[statusCode] || 'bg-gray-100 text-gray-800'
    }

    // 새로운 badge 스타일 정의
    const getBadgeStyle = (type) => {
        switch (type) {
            case 'order':
                return 'bg-indigo-100 text-indigo-800 border-indigo-200'
            case 'serious':
                return 'bg-red-100 text-red-800 border-red-200'
            case 'defectDiv':
                return 'bg-emerald-100 text-emerald-800 border-emerald-200'
            default:
                return 'bg-gray-100 text-gray-800 border-gray-200'
        }
    }

    const handleFileUpload = (event) => {
        const file = event.target.files[0]
        if (file) {
            // 파일 크기 체크 (10MB = 10 * 1024 * 1024 bytes)
            const maxSize = 10 * 1024 * 1024
            if (file.size > maxSize) {
                alert('파일 크기는 10MB를 초과할 수 없습니다.')
                return
            }

            onFileChange(file) // 상위 컴포넌트로 파일 전달
        }
    }

    const removeFile = () => {
        onFileChange(null) // 상위 컴포넌트에 파일 제거 알림
        // 파일 input 초기화
        const fileInput = document.getElementById('file-upload')
        if (fileInput) {
            fileInput.value = ''
        }
    }

    const formatFileSize = (bytes) => {
        if (bytes === 0) return '0 Bytes'
        const k = 1024
        const sizes = ['Bytes', 'KB', 'MB', 'GB']
        const i = Math.floor(Math.log(bytes) / Math.log(k))
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
    }

    // Content-Disposition 헤더에서 파일명 추출
    const extractFileNameFromHeader = (contentDisposition) => {
        if (!contentDisposition) return null

        // filename*=UTF-8''encoded_filename 형식 우선 처리
        const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/)
        if (utf8Match) {
            return decodeURIComponent(utf8Match[1])
        }

        // filename="filename" 형식 처리
        const filenameMatch = contentDisposition.match(/filename="([^"]+)"/)
        if (filenameMatch) {
            return filenameMatch[1]
        }

        return null
    }

    // axios를 사용한 파일 다운로드 핸들러
    const handleFileDownload = async (fileName) => {
        try {
            const response = await axios.get(
                `${apiPrefix}/files/download/${fileName}`,
                {
                    responseType: 'blob',
                    withCredentials: true,
                    headers: {
                        Accept: 'application/octet-stream',
                    },
                },
            )

            // 서버에서 설정한 Content-Disposition 헤더에서 파일명 추출
            const contentDisposition = response.headers['content-disposition']
            const downloadFileName =
                extractFileNameFromHeader(contentDisposition) || fileName

            // Blob 생성 및 다운로드 링크 생성
            const blob = new Blob([response.data])
            const downloadUrl = window.URL.createObjectURL(blob)

            // 가상의 링크 생성 후 클릭으로 다운로드 실행
            const link = document.createElement('a')
            link.href = downloadUrl
            link.setAttribute('download', downloadFileName)
            document.body.appendChild(link)
            link.click()

            // 정리 작업
            document.body.removeChild(link)
            window.URL.revokeObjectURL(downloadUrl)
        } catch (error) {
            console.error('파일 다운로드 실패:', error)
            if (error.response?.status === 404) {
                alert('파일을 찾을 수 없습니다.')
            } else {
                alert('파일 다운로드에 실패했습니다.')
            }
        }
    }

    // 파일 목록 렌더링 함수
    const renderDefectFiles = (defectLogFiles) => {
        if (!defectLogFiles || defectLogFiles.length === 0) {
            return null
        }

        return (
            <div className="mt-3 pt-3 border-t border-gray-200">
                <div className="text-sm font-medium text-gray-700 mb-2">
                    첨부파일
                </div>
                <div className="space-y-1">
                    {Array.from(defectLogFiles).map((file, fileIndex) => (
                        <div
                            key={fileIndex}
                            className="flex items-center gap-2"
                        >
                            <HiOutlineDocumentText className="text-blue-500 w-4 h-4 flex-shrink-0" />
                            <button
                                onClick={() =>
                                    handleFileDownload(file.sys_file_name)
                                }
                                className="text-sm text-blue-600 hover:text-blue-800 hover:underline cursor-pointer text-left"
                            >
                                {file.org_file_name}
                            </button>
                        </div>
                    ))}
                </div>
            </div>
        )
    }

    return (
        <div className="w-full h-full">
            {/* 결함 정보 헤더 */}
            {assigneeInfo && (
                <div className="mb-4 p-4 bg-white border border-gray-200 rounded-lg shadow-sm">
                    {/* Badge 영역 추가 */}
                    <div className="flex items-center gap-2 mb-3">
                        {assigneeInfo.seriousCode && (
                            <span
                                className={`px-2 py-1 text-xs rounded-md font-medium border ${getBadgeStyle('order')}`}
                            >
                                {assigneeInfo.seriousCode}
                            </span>
                        )}
                        {assigneeInfo.orderCode && (
                            <span
                                className={`px-2 py-1 text-xs rounded-md font-medium border ${getBadgeStyle('serious')}`}
                            >
                                {assigneeInfo.orderCode}
                            </span>
                        )}
                        {assigneeInfo.defectDivCode && (
                            <span
                                className={`px-2 py-1 text-xs rounded-md font-medium border ${getBadgeStyle('defectDiv')}`}
                            >
                                {assigneeInfo.defectDivCode}
                            </span>
                        )}
                    </div>
                    <div className="flex items-center gap-2 mb-2">
                        <span className="text-sm font-medium text-gray-600">
                            [{assigneeInfo.customerName}]
                        </span>
                        <h4 className="text-lg font-semibold text-gray-900">
                            {assigneeInfo.defectTitle}
                        </h4>
                    </div>

                    <div className="flex items-center gap-2">
                        {assigneeInfo.defectUrlInfo ? (
                            <a
                                href={assigneeInfo.defectUrlInfo}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="flex items-center gap-2 text-sm text-blue-600 hover:text-blue-800 hover:underline"
                            >
                                <HiOutlineExternalLink className="text-gray-400 w-4 h-4" />
                                <span>{assigneeInfo.defectMenuTitle}</span>
                                <span>({assigneeInfo.defectUrlInfo})</span>
                            </a>
                        ) : (
                            <div className="flex items-center gap-2">
                                <HiOutlineExternalLink className="text-gray-400 w-4 h-4" />
                                <span className="text-sm text-gray-600">
                                    {assigneeInfo.defectMenuTitle}
                                </span>
                            </div>
                        )}
                    </div>
                </div>
            )}

            <Card className="w-full max-w-none h-full bg-blue-50">
                <div className="w-full h-full p-6">
                    <h5 className="text-xl font-semibold mb-6">처리 이력</h5>
                    <div
                        className={`w-full ${defectLogs.length > 2 ? 'max-h-[400px] overflow-y-auto' : 'min-h-[300px]'}`}
                    >
                        <Timeline>
                            {defectLogs.length > 0 ? (
                                defectLogs.map((log, index) => (
                                    <Timeline.Item key={log.logSeq || index}>
                                        <div className="flex items-center gap-2 mb-2">
                                            {log.statusCode && (
                                                <span
                                                    className={`px-2 py-1 text-xs rounded-full font-medium ${getStatusBadge(log.statusCode)}`}
                                                >
                                                    {log.statusCode}
                                                </span>
                                            )}
                                        </div>
                                        <div className="text-base text-gray-500 mb-3">
                                            {formatDate(log.createdAt)} -{' '}
                                            {log.createdBy}
                                        </div>
                                        <div className="text-lg font-medium p-1 rounded-lg">
                                            {log.logCt}
                                        </div>
                                        {/* 파일 목록 추가 */}
                                        {renderDefectFiles(log.defectLogFiles)}
                                    </Timeline.Item>
                                ))
                            ) : (
                                <Timeline.Item>
                                    <div className="text-gray-500 text-lg">
                                        처리 이력이 없습니다.
                                    </div>
                                </Timeline.Item>
                            )}
                        </Timeline>
                    </div>
                </div>
            </Card>

            {/* 조치 내역 입력 섹션 */}
            <div className="mt-6 p-4 bg-white border border-gray-200 rounded-lg">
                <h6 className="text-lg font-semibold text-gray-800 mb-4">
                    조치 내역 입력
                </h6>

                {/* Textarea 영역 */}
                <div className="mb-4">
                    <textarea
                        id="action-comment"
                        value={logCt}
                        onChange={(e) => onLogCtChange(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                        rows={5}
                    />
                </div>

                {/* 파일 업로드 영역 - 단일 파일만 업로드 */}
                <div className="mb-4">
                    <div className="border-2 border-dashed border-gray-300 rounded-lg p-4 hover:border-gray-400 transition-colors">
                        {uploadedFile ? (
                            <div className="flex items-center justify-between bg-gray-50 p-3 rounded-md">
                                <div className="flex items-center gap-3">
                                    <HiOutlineDocumentText className="text-blue-500 w-6 h-6 flex-shrink-0" />
                                    <div className="flex-1 min-w-0">
                                        <p className="text-sm font-medium text-gray-900 truncate">
                                            {uploadedFile.name}
                                        </p>
                                        <p className="text-xs text-gray-500">
                                            {formatFileSize(uploadedFile.size)}
                                        </p>
                                    </div>
                                </div>
                                <button
                                    onClick={removeFile}
                                    className="ml-4 px-3 py-1 bg-red-100 text-red-700 text-sm font-medium rounded-md hover:bg-red-200 transition-colors"
                                >
                                    제거
                                </button>
                            </div>
                        ) : (
                            <div className="text-center">
                                <div className="text-sm text-gray-600 mb-2">
                                    파일을 선택하여 업로드하세요
                                </div>
                                <input
                                    id="file-upload"
                                    type="file"
                                    className="hidden"
                                    onChange={handleFileUpload}
                                    accept=".jpg,.jpeg,.png,.gif,.pdf,.doc,.docx,.xls,.xlsx,.txt"
                                />
                                <label
                                    htmlFor="file-upload"
                                    className="inline-block px-6 py-2 bg-blue-500 text-white text-sm font-medium rounded-md hover:bg-blue-600 cursor-pointer transition-colors"
                                >
                                    파일 선택
                                </label>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* 결함 처리 가이드 섹션 */}
            <div className="mt-6 p-4 bg-orange-50 border border-orange-200 rounded-lg">
                <div className="mb-4">
                    <h6 className="text-lg font-semibold text-orange-800 mb-2">
                        결함 처리 담당자
                    </h6>
                    <div className="flex items-center gap-2">
                        <span className="text-gray-600">
                            {assigneeInfo?.assignUserName || '-'} (
                            {assigneeInfo?.assignUserId || '-'}) 님이
                            할당되었습니다.
                        </span>
                    </div>
                </div>

                <div className="space-y-3">
                    <h6 className="text-lg font-semibold text-orange-800">
                        처리 가이드
                    </h6>
                    <div className="space-y-2 text-sm">
                        <div className="flex items-start gap-2">
                            <span className="w-2 h-2 bg-orange-500 rounded-full mt-2 flex-shrink-0"></span>
                            <span>
                                결함 조치 후, 조치 내역과 (필요한 경우)
                                첨부파일을 등록하시기 바랍니다.
                            </span>
                        </div>
                        <div className="flex items-start gap-2">
                            <span className="w-2 h-2 bg-orange-500 rounded-full mt-2 flex-shrink-0"></span>
                            <span>
                                결함이 아닌 경우, 사유 입력 후{' '}
                                <strong>결함조치 보류(결함아님)</strong> 상태로
                                변경하시기 바랍니다. Q/A팀에서 확인 후 종결처리
                                합니다.
                            </span>
                        </div>
                        <div className="flex items-start gap-2">
                            <span className="w-2 h-2 bg-orange-500 rounded-full mt-2 flex-shrink-0"></span>
                            <span>
                                개발을 위해 임시로 허용한 결함인 경우,{' '}
                                <strong>To-Do 처리</strong> 상태로 변경하시기
                                바랍니다. Q/A팀에서 확인 후 To-Do 목록으로
                                이관합니다.
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default DefectTimeline