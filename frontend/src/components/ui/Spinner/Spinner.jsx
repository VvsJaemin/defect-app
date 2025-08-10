import  { forwardRef } from 'react'
import classNames from 'classnames'
import { CgSpinner } from 'react-icons/cg'

const Spinner = forwardRef((props, ref) => {
    const {
        className,
        customColorClass,
        enableTheme = true,
        indicator: Component = CgSpinner,
        isSpinning = true, // 오타 수정: isSpining → isSpinning
        size = 20,
        style,
        ...rest
    } = props

    const spinnerColor = customColorClass || (enableTheme && 'text-primary')

    const spinnerStyle = {
        height: size,
        width: size,
        ...style,
    }

    const spinnerClass = classNames(
        isSpinning && 'animate-spin', // 오타 수정
        spinnerColor,
        className,
    )

    return (
        <Component
            ref={ref}
            style={spinnerStyle}
            className={spinnerClass}
            {...rest}
        />
    )
})

Spinner.displayName = 'Spinner'

export default Spinner