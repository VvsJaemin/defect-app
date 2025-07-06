import { useState, useEffect, useRef } from 'react'
import Card from '@/components/ui/Card'
import Select from '@/components/ui/Select'
import Chart from '@/components/shared/Chart'
import { useThemeStore } from '@/store/themeStore'
import classNames from '@/utils/classNames'
import { NumericFormat } from 'react-number-format'
import { TbBug, TbCheckbox, TbX, TbCheck, TbTargetArrow } from 'react-icons/tb'

const StatisticCard = (props) => {
    const {
        title,
        value,
        label,
        icon,
        iconClass,
        active,
        onClick,
        suffix = '',
        isPercentage = false,
    } = props

    return (
        <button
            className={classNames(
                'p-4 rounded-2xl cursor-pointer ltr:text-left rtl:text-right transition duration-150 outline-hidden',
                active && 'bg-white dark:bg-gray-900 shadow-md',
            )}
            onClick={() => onClick(label)}
        >
            <div className="flex items-center gap-3">
                <div
                    className={classNames(
                        'flex items-center justify-center min-h-12 min-w-12 max-h-12 max-w-12 text-gray-900 rounded-full text-2xl',
                        iconClass,
                    )}
                >
                    {icon}
                </div>
                <div className="flex-1">
                    <div className="mb-2 text-sm font-semibold">{title}</div>
                    <h3 className="mb-1">
                        <NumericFormat
                            displayType="text"
                            value={value}
                            thousandSeparator={true}
                            suffix={suffix}
                            decimalScale={isPercentage ? 1 : 0}
                        />
                    </h3>
                </div>
            </div>
        </button>
    )

}

const DefectOverview = ({ data = {}, weeklyData = [] }) => {
    const [selectedCategory, setSelectedCategory] = useState('today')

    const sideNavCollapse = useThemeStore(
        (state) => state.layout.sideNavCollapse,
    )

    const isFirstRender = useRef(true)

    useEffect(() => {
        if (!sideNavCollapse && isFirstRender.current) {
            isFirstRender.current = false
            return
        }

        if (!isFirstRender.current) {
            window.dispatchEvent(new Event('resize'))
        }
    }, [sideNavCollapse])

    // 차트 색상 정의
    const chartColors = {
        defectCompleted: '#1E3A8A', // 빨간색 - 조치완료
        defectOccurred: '#10B981'  // 초록색 - 발생 결함
    }
    // weeklyData를 기반으로 차트 데이터 생성
    const generateChartDataFromWeekly = () => {
        if (!weeklyData || weeklyData.length === 0) {
            return {
                dates: [],
                series: [
                    { name: '조치 완료', data: [] },
                    { name: '발생 결함', data: [] },
                ],
            }
        }

        const formatDate = (dateStr) => {
            const date = new Date(dateStr)
            if (isNaN(date.getTime())) return ''
            const month = (date.getMonth() + 1).toString().padStart(2, '0')
            const day = date.getDate().toString().padStart(2, '0')
            return `${month}/${day}`
        }

        const startDate = formatDate(weeklyData[0].startDate)
        const endDate = formatDate(weeklyData[0].endDate)

        const defectDateMap = new Map()

        weeklyData.forEach((item) => {
            const dateKey = formatDate(item.defectDate)
            if (!defectDateMap.has(dateKey)) {
                defectDateMap.set(dateKey, {
                    total: item.totalDefects || 0,
                    completed: item.completedDefects || 0,
                })
            } else {
                // 중복 날짜가 있을 경우 누적 처리
                const existing = defectDateMap.get(dateKey)
                defectDateMap.set(dateKey, {
                    total: existing.total + (item.totalDefects || 0),
                    completed: existing.completed + (item.completedDefects || 0),
                })
            }
        })

        // x축 날짜 리스트 구성
        const xLabelsSet = new Set([
            startDate,
            ...[...defectDateMap.keys()],
            endDate,
        ])
        const xLabels = [...xLabelsSet].sort((a, b) => new Date(`2025/${a}`) - new Date(`2025/${b}`))

        // 각 날짜에 맞는 값 채우기
        const totalDefectsData = []
        const completedDefectsData = []

        xLabels.forEach((date) => {
            const data = defectDateMap.get(date)
            totalDefectsData.push(data?.total || 0)
            completedDefectsData.push(data?.completed || 0)
        })

        return {
            dates: xLabels,
            series: [
                {
                    name: '조치 완료',
                    data: completedDefectsData,
                },
                {
                    name: '발생 결함',
                    data: totalDefectsData,
                },
            ],
        }
    }

    const chartData = generateChartDataFromWeekly()

    // StatisticData 구조에 맞춰 데이터 매핑
    const statisticData = [
        {
            title: '오늘 발생 결함',
            value: data.todayTotalDefect || 0,
            icon: <TbBug />,
            iconClass: 'bg-red-200',
            label: 'todayTotalDefect',
            suffix: ' 건',
            isPercentage: false
        },
        {
            title: '오늘 처리 결함',
            value: data.todayProcessedDefect || 0,
            icon: <TbCheckbox />,
            iconClass: 'bg-green-200',
            label: 'todayProcessedDefect',
            suffix: ' 건',
            isPercentage: false
        },
        {
            title: '오늘 조치율',
            value: data.todayProcessRate || 0,
            icon: <TbTargetArrow />,
            iconClass: 'bg-blue-200',
            label: 'todayProcessRate',
            suffix: '%',
            isPercentage: true
        }
    ]

    const cumulativeData = [
        {
            title: '누적 총 결함',
            value: data.totalDefect || 0,
            icon: <TbBug />,
            iconClass: 'bg-orange-200',
            label: 'totalDefect',
            suffix: ' 건',
            isPercentage: false
        },
        {
            title: '누적 해제',
            value: data.defectCanceled || 0,
            icon: <TbX />,
            iconClass: 'bg-gray-200',
            label: 'defectCanceled',
            suffix: ' 건',
            isPercentage: false
        },
        {
            title: '누적 종료',
            value: data.defectClosed || 0,
            icon: <TbCheck />,
            iconClass: 'bg-emerald-200',
            label: 'defectClosed',
            suffix: ' 건',
            isPercentage: false
        },
        {
            title: '해제율',
            value: data.cancelRate || 0,
            icon: <TbX />,
            iconClass: 'bg-purple-200',
            label: 'cancelRate',
            suffix: '%',
            isPercentage: true
        },
        {
            title: '종료율',
            value: data.closeRate || 0,
            icon: <TbCheck />,
            iconClass: 'bg-indigo-200',
            label: 'closeRate',
            suffix: '%',
            isPercentage: true
        }
    ]

    return (
        <Card>
            <div className="flex items-center justify-between">
                <h4>결함 현황</h4>
                <Select
                    className="w-[120px]"
                    size="sm"
                    placeholder="기간 선택"
                    value={{ value: selectedCategory, label: selectedCategory === 'today' ? '오늘' : '누적' }}
                    options={[
                        { value: 'today', label: '오늘' },
                        { value: 'cumulative', label: '누적' }
                    ]}
                    isSearchable={false}
                    onChange={(option) => {
                        if (option?.value) {
                            setSelectedCategory(option?.value)
                        }
                    }}
                />
            </div>

            {selectedCategory === 'today' && (
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 rounded-2xl p-3 bg-gray-100 dark:bg-gray-700 mt-4">
                    {statisticData.map((stat) => (
                        <StatisticCard
                            key={stat.label}
                            title={stat.title}
                            value={stat.value}
                            icon={stat.icon}
                            iconClass={stat.iconClass}
                            label={stat.label}
                            active={false}
                            onClick={() => {}}
                            suffix={stat.suffix}
                            isPercentage={stat.isPercentage}
                        />
                    ))}
                </div>
            )}

            {selectedCategory === 'cumulative' && (
                <div className="grid grid-cols-1 md:grid-cols-5 gap-4 rounded-2xl p-3 bg-gray-100 dark:bg-gray-700 mt-4">
                    {cumulativeData.map((stat) => (
                        <StatisticCard
                            key={stat.label}
                            title={stat.title}
                            value={stat.value}
                            icon={stat.icon}
                            iconClass={stat.iconClass}
                            label={stat.label}
                            active={false}
                            onClick={() => {}}
                            suffix={stat.suffix}
                            isPercentage={stat.isPercentage}
                        />
                    ))}
                </div>
            )}

            <div className="mt-6 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
                <h5 className="text-sm font-semibold mb-2">요약 정보</h5>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                    <div>
                        <span className="text-gray-500">오늘 발생: </span>
                        <span className="font-semibold">{data.todayTotalDefect || 0}건</span>
                    </div>
                    <div>
                        <span className="text-gray-500">오늘 처리: </span>
                        <span className="font-semibold">{data.todayProcessedDefect || 0}건</span>
                    </div>
                    <div>
                        <span className="text-gray-500">누적 총계: </span>
                        <span className="font-semibold">{data.totalDefect || 0}건</span>
                    </div>
                    <div>
                        <span className="text-gray-500">전체 종료율: </span>
                        <span className="font-semibold">
                            <NumericFormat
                                displayType="text"
                                value={data.closeRate || 0}
                                decimalScale={1}
                                suffix="%"
                            />
                        </span>
                    </div>
                </div>
            </div>

            {/* 주간 결함 발생 / 조치완료 추이 차트 */}
            <div className="mt-6">
                <div className="flex items-center justify-between mb-4">
                    <h5 className="text-lg font-semibold">결함 발생 / 완료 추이</h5>
                    {weeklyData && weeklyData.length > 0 && (
                        <span className="text-sm text-gray-500">
                            최근 {weeklyData.length}주간 데이터
                        </span>
                    )}
                </div>

                <Chart
                    type="line"
                    series={chartData.series}
                    height="410px"
                    customOptions={{
                        legend: {
                            show: true,
                            position: 'top',
                            horizontalAlign: 'center',
                            fontSize: '14px',
                            fontFamily: 'inherit'
                        },
                        colors: [chartColors.defectCompleted, chartColors.defectOccurred],
                        stroke: {
                            curve: 'smooth',
                            width: 3
                        },
                        markers: {
                            size: 5,
                            strokeWidth: 2,
                            strokeColors: '#fff',
                            fillColors: [chartColors.defectCompleted, chartColors.defectOccurred]
                        },
                        grid: {
                            borderColor: '#f1f5f9',
                            strokeDashArray: 5
                        },
                        tooltip: {
                            y: {
                                formatter: function (val) {
                                    return val + '건'
                                }
                            }
                        },
                        yaxis: {
                            title: {
                                text: '',
                                style: {
                                    fontSize: '12px',
                                    fontFamily: 'inherit'
                                }
                            },
                            labels: {
                                formatter: function (val) {
                                    return Math.floor(val) + '건'
                                }
                            }
                        },
                        xaxis: {
                            title: {
                                text: '',
                                style: {
                                    fontSize: '12px',
                                    fontFamily: 'inherit'
                                }
                            },
                            categories: chartData.dates,
                            labels: {
                                style: {
                                    fontSize: '11px'
                                }
                            }
                        }
                    }}
                />
            </div>
        </Card>
    )
}

export default DefectOverview