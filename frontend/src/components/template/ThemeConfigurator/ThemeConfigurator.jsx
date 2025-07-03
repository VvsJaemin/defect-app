import ModeSwitcher from './ModeSwitcher'
import ThemeSwitcher from './ThemeSwitcher'
import DirectionSwitcher from './DirectionSwitcher'

const ThemeConfigurator = ({ callBackClose }) => {
    return (
        <div className="flex flex-col h-full justify-between">
            <div className="flex flex-col gap-y-10 mb-6">
                <div className="flex items-center justify-between">
                    <div>
                        <h6>다크 모드</h6>
                        <span>화면을 어두운 테마로 변경합니다</span>
                    </div>
                    <ModeSwitcher />
                </div>
                <div className="flex items-center justify-between">
                    <div>
                        <h6>화면 방향</h6>
                        <span>왼쪽에서 오른쪽 또는 오른쪽에서 왼쪽으로 변경</span>
                    </div>
                    <DirectionSwitcher callBackClose={callBackClose} />
                </div>
                <div>
                    <h6 className="mb-3">테마 색상</h6>
                    <ThemeSwitcher />
                </div>
                <div>
                    {/*<h6 className="mb-3">레이아웃 설정</h6>*/}
                    {/*<LayoutSwitcher />*/}
                </div>
            </div>
            {/*<CopyButton />*/}
        </div>
    )
}

export default ThemeConfigurator
