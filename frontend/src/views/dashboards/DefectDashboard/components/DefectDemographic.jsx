import { useState } from 'react'
import Card from '@/components/ui/Card'
import Progress from '@/components/ui/Progress'
import classNames from '@/utils/classNames'
import { NumericFormat } from 'react-number-format'
import { TbBug, TbCheck, TbX, TbClock, TbTargetArrow } from 'react-icons/tb'

const DefectStatusCard = ({
                              title,
                              value,
                              percentage,
                              icon,
                              iconClass,
                              active,
                              onClick,
                              suffix = '건'
                          }) => {
    const hasPercentage = percentage !== undefined && percentage !== null

    return (
        <div
            className={classNames(
                'flex items-center gap-4 p-4 rounded-xl transition-colors duration-150 cursor-pointer',
                active && 'bg-gray-100 dark:bg-gray-700',
            )}
            onClick={onClick}
        >
            <div className={classNames(
                'flex items-center justify-center w-12 h-12 rounded-full text-xl',
                iconClass
            )}>
                {icon}
            </div>
            <div className="flex-1">
                <div className="heading-text font-semibold mb-1">
                    {title}
                </div>
                <div className="flex items-center gap-2 mb-2">
                    <NumericFormat
                        displayType="text"
                        value={value}
                        thousandSeparator={true}
                        suffix={suffix}
                        className="text-lg font-bold"
                    />
                    {hasPercentage && (
                        <span className="text-sm text-gray-500">
                            ({percentage}%)
                        </span>
                    )}
                </div>
                {hasPercentage && (
                    <Progress
                        percent={percentage}
                        trailClass={classNames(
                            'transition-colors duration-150',
                            active && 'bg-gray-200 dark:bg-gray-600',
                        )}
                    />
                )}
            </div>
        </div>
    )
}

const DefectDemographic = ({ data = {} }) => {
    const [hovering, setHovering] = useState('')

    // 총 결함 수 계산 (분모)
    const totalDefects = data.totalDefect || 1

    // 퍼센트 계산 헬퍼 함수 (소수점 첫째자리까지)
    const calculatePercentage = (value, total) => {
        return parseFloat(((value / total) * 100).toFixed(1))
    }

    // 각 상태별 데이터 준비
    const statusData = [
        {
            id: 'total',
            title: '총 결함',
            value: data.totalDefect || 0,
            // percentage: undefined (퍼센트 없음)
            icon: <TbBug />,
            iconClass: 'bg-red-200 text-red-600',
        },
        {
            id: 'closed',
            title: '종료된 결함',
            value: data.defectClosed || 0,
            percentage: calculatePercentage(data.defectClosed || 0, totalDefects),
            icon: <TbCheck />,
            iconClass: 'bg-green-200 text-green-600'
        },
        {
            id: 'canceled',
            title: '해제된 결함',
            value: data.defectCanceled || 0,
            percentage: calculatePercentage(data.defectCanceled || 0, totalDefects),
            icon: <TbX />,
            iconClass: 'bg-gray-200 text-gray-600'
        },
        {
            id: 'inProgress',
            title: '진행 중인 결함',
            value: (data.totalDefect || 0) - (data.defectClosed || 0) - (data.defectCanceled || 0),
            percentage: calculatePercentage(
                (data.totalDefect || 0) - (data.defectClosed || 0) - (data.defectCanceled || 0),
                totalDefects
            ),
            icon: <TbClock />,
            iconClass: 'bg-blue-200 text-blue-600'
        }
    ]

    // 오늘 통계 데이터
    const todayData = [
        {
            id: 'todayTotal',
            title: '오늘 발생',
            value: data.todayTotalDefect || 0,
            // percentage: undefined (퍼센트 없음)
            icon: <TbBug />,
            iconClass: 'bg-orange-200 text-orange-600'
        },
        {
            id: 'todayProcessed',
            title: '오늘 처리',
            value: data.todayProcessedDefect || 0,
            percentage: parseFloat((data.todayProcessRate || 0).toFixed(1)),
            icon: <TbTargetArrow />,
            iconClass: 'bg-emerald-200 text-emerald-600'
        }
    ]

    const [selectedView, setSelectedView] = useState('cumulative')

    return (
        <Card>
            <div className="flex items-center justify-between mb-4">
                <h4>결함 상태 분포</h4>
                <div className="flex gap-2">
                    <button
                        className={classNames(
                            'px-3 py-1 rounded-md text-sm transition-colors',
                            selectedView === 'cumulative'
                                ? 'bg-primary text-white'
                                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                        )}
                        onClick={() => setSelectedView('cumulative')}
                    >
                        누적
                    </button>
                    <button
                        className={classNames(
                            'px-3 py-1 rounded-md text-sm transition-colors',
                            selectedView === 'today'
                                ? 'bg-primary text-white'
                                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                        )}
                        onClick={() => setSelectedView('today')}
                    >
                        오늘
                    </button>
                </div>
            </div>

            <div className="flex flex-col gap-2">
                {selectedView === 'cumulative' ? (
                    statusData.map((item) => (
                        <DefectStatusCard
                            key={item.id}
                            title={item.title}
                            value={item.value}
                            percentage={item.percentage}
                            icon={item.icon}
                            iconClass={item.iconClass}
                            active={hovering === item.id}
                            onClick={() => setHovering(hovering === item.id ? '' : item.id)}
                        />
                    ))
                ) : (
                    todayData.map((item) => (
                        <DefectStatusCard
                            key={item.id}
                            title={item.title}
                            value={item.value}
                            percentage={item.percentage}
                            icon={item.icon}
                            iconClass={item.iconClass}
                            active={hovering === item.id}
                            onClick={() => setHovering(hovering === item.id ? '' : item.id)}
                        />
                    ))
                )}
            </div>

            <div className="mt-6 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
                <h5 className="text-sm font-semibold mb-3">주요 지표</h5>
                <div className="grid grid-cols-2 gap-4 text-sm">
                    <div className="flex justify-between">
                        <span className="text-gray-500">종료율:</span>
                        <span className="font-semibold text-green-600">
                            {parseFloat((data.closeRate || 0).toFixed(1))}%
                        </span>
                    </div>
                    <div className="flex justify-between">
                        <span className="text-gray-500">해제율:</span>
                        <span className="font-semibold text-gray-600">
                            {parseFloat((data.cancelRate || 0).toFixed(1))}%
                        </span>
                    </div>
                    <div className="flex justify-between">
                        <span className="text-gray-500">오늘 조치율:</span>
                        <span className="font-semibold text-blue-600">
                            {parseFloat((data.todayProcessRate || 0).toFixed(1))}%
                        </span>
                    </div>
                    <div className="flex justify-between">
                        <span className="text-gray-500">미처리:</span>
                        <span className="font-semibold text-orange-600">
                            {((data.totalDefect || 0) - (data.defectClosed || 0) - (data.defectCanceled || 0)) || 0}건
                        </span>
                    </div>
                </div>
            </div>
        </Card>
    )
}

export default DefectDemographic