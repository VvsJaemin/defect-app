
import Card from '@/components/ui/Card'
import Timeline from '@/components/ui/Timeline'
import dayjs from 'dayjs'
import { HiOutlineExternalLink } from 'react-icons/hi'

const DefectTimeline = ({ data }) => {
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
            '결함등록': 'bg-blue-100 text-blue-800',
            '처리중': 'bg-yellow-100 text-yellow-800',
            '완료': 'bg-green-100 text-green-800',
            '보류': 'bg-gray-100 text-gray-800',
            '이관': 'bg-purple-100 text-purple-800'
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

    return (
        <div className="w-full h-full">
            {/* 결함 정보 헤더 */}
            {assigneeInfo && (
                <div className="mb-4 p-4 bg-white border border-gray-200 rounded-lg shadow-sm">
                    {/* Badge 영역 추가 */}
                    <div className="flex items-center gap-2 mb-3">
                        {assigneeInfo.seriousCode && (
                            <span className={`px-2 py-1 text-xs rounded-md font-medium border ${getBadgeStyle('order')}`}>
                                {assigneeInfo.seriousCode}
                            </span>
                        )}
                        {assigneeInfo.orderCode && (
                            <span className={`px-2 py-1 text-xs rounded-md font-medium border ${getBadgeStyle('serious')}`}>
                                {assigneeInfo.orderCode}
                            </span>
                        )}
                        {assigneeInfo.defectDivCode && (
                            <span className={`px-2 py-1 text-xs rounded-md font-medium border ${getBadgeStyle('defectDiv')}`}>
                                {assigneeInfo.defectDivCode}
                            </span>
                        )}
                    </div>
                    <div className="flex items-center gap-2 mb-2">
                        <span className="text-sm font-medium text-gray-600">[{assigneeInfo.customerName}]</span>
                        <h4 className="text-lg font-semibold text-gray-900">{assigneeInfo.defectTitle}</h4>
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
                                <span className="text-sm text-gray-600">{assigneeInfo.defectMenuTitle}</span>
                            </div>
                        )}
                    </div>
                </div>
            )}

            <Card className="w-full max-w-none h-full bg-blue-50">
                <div className="w-full h-full p-6">
                    <h5 className="text-xl font-semibold mb-6">처리 이력</h5>
                    <div className={`w-full ${defectLogs.length > 2 ? 'max-h-[400px] overflow-y-auto' : 'min-h-[300px]'}`}>
                        <Timeline>
                            {defectLogs.length > 0 ? (
                                defectLogs.map((log, index) => (
                                    <Timeline.Item key={log.logSeq || index}>
                                        <div className="flex items-center gap-2 mb-2">
                                            <div className="font-semibold text-lg">
                                                {log.logTitle}
                                            </div>
                                            {log.statusCode && (
                                                <span className={`px-2 py-1 text-xs rounded-full font-medium ${getStatusBadge(log.statusCode)}`}>
                                                    {log.statusCode}
                                                </span>
                                            )}
                                        </div>
                                        <div className="text-base text-gray-500 mb-3">
                                            {formatDate(log.createdAt)} - {log.createdBy}
                                        </div>
                                        <div className="text-lg font-medium p-1 rounded-lg ">
                                            {log.logCt}
                                        </div>
                                    </Timeline.Item>
                                ))
                            ) : (
                                <Timeline.Item>
                                    <div className="text-gray-500 text-lg">처리 이력이 없습니다.</div>
                                </Timeline.Item>
                            )}
                        </Timeline>
                    </div>
                </div>
            </Card>
            {/* 결함 처리 가이드 섹션 */}
            <div className="mt-6 p-4 bg-orange-50 border border-orange-200 rounded-lg">
                <div className="mb-4">
                    <h6 className="text-lg font-semibold text-orange-800 mb-2">결함 처리 담당자</h6>
                    <div className="flex items-center gap-2">
                        <span className="text-gray-600">{assigneeInfo?.assignUserName || '-'} ({assigneeInfo?.assignUserId || '-'}) 님이 할당되었습니다.</span>
                    </div>
                </div>

                <div className="space-y-3">
                    <h6 className="text-lg font-semibold text-orange-800">처리 가이드</h6>
                    <div className="space-y-2 text-sm">
                        <div className="flex items-start gap-2">
                            <span className="w-2 h-2 bg-orange-500 rounded-full mt-2 flex-shrink-0"></span>
                            <span>결함 조치 후, 조치 내역과 (필요한 경우) 첨부파일을 등록하시기 바랍니다.</span>
                        </div>
                        <div className="flex items-start gap-2">
                            <span className="w-2 h-2 bg-orange-500 rounded-full mt-2 flex-shrink-0"></span>
                            <span>결함이 아닌 경우, 사유 입력 후 <strong>결함조치 보류(결함아님)</strong> 상태로 변경하시기 바랍니다. Q/A팀에서 확인 후 종결처리 합니다.</span>
                        </div>
                        <div className="flex items-start gap-2">
                            <span className="w-2 h-2 bg-orange-500 rounded-full mt-2 flex-shrink-0"></span>
                            <span>개발을 위해 임시로 허용한 결함인 경우, <strong>To-Do 처리</strong> 상태로 변경하시기 바랍니다. Q/A팀에서 확인 후 To-Do 목록으로 이관합니다.</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default DefectTimeline