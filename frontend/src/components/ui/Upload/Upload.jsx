import { useRef, useState, useCallback, useEffect } from 'react'
import classNames from '../utils/classNames'
import cloneDeep from 'lodash/cloneDeep'
import FileItem from './FileItem'
import Button from '../Button/Button'
import CloseButton from '../CloseButton'
import Notification from '../Notification/Notification'
import toast from '../toast/toast'

const filesToArray = (files) => Object.keys(files).map((key) => files[key])

const Upload = (props) => {
    const {
        accept,
        beforeUpload,
        disabled = false,
        draggable = false,
        fileList = [],
        existingFiles = [], // 기존 파일 목록 추가
        onExistingFileRemove, // 기존 파일 삭제 콜백 추가
        fileListClass,
        fileItemClass,
        multiple,
        onChange,
        onFileRemove,
        ref,
        showList = true,
        tip,
        uploadLimit,
        children,
        className,
        ...rest
    } = props

    const fileInputField = useRef(null)
    const [files, setFiles] = useState(fileList)
    const [dragOver, setDragOver] = useState(false)

    useEffect(() => {
        if (JSON.stringify(files) !== JSON.stringify(fileList)) {
            setFiles(fileList)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [JSON.stringify(fileList)])

    const triggerMessage = (msg = '') => {
        toast.push(
            <Notification type="danger" duration={2000}>
                {msg || 'Upload Failed!'}
            </Notification>,
            {
                placement: 'top-center',
            },
        )
    }

    const pushFile = (newFiles, file) => {
        if (newFiles) {
            for (const f of newFiles) {
                file.push(f)
            }
        }

        return file
    }

    const addNewFiles = (newFiles) => {
        let file = cloneDeep(files)

        if (typeof uploadLimit === 'number' && uploadLimit !== 0) {
            const currentCount = file.length + existingFiles.length // 기존 파일 개수도 포함
            const newCount = newFiles.length
            const totalCount = currentCount + newCount

            if (totalCount > uploadLimit) {
                // 제한 초과 시 에러 메시지 표시
                triggerMessage(`최대 ${uploadLimit}개의 파일만 업로드할 수 있습니다.`)
                return file // 기존 파일 목록만 반환 (새 파일 추가하지 않음)
            }

            if (currentCount >= uploadLimit) {
                if (uploadLimit === 1) {
                    file.shift()
                    file = pushFile(newFiles, file)
                }
                return filesToArray({ ...file })
            }
        }

        file = pushFile(newFiles, file)
        return filesToArray({ ...file })
    }

    const onNewFileUpload = (e) => {
        const { files: newFiles } = e.target
        let result = true

        if (beforeUpload) {
            result = beforeUpload(newFiles, files)

            if (result === false) {
                triggerMessage()
                return
            }

            if (typeof result === 'string' && result.length > 0) {
                triggerMessage(result)
                return
            }
        }

        if (result) {
            const updatedFiles = addNewFiles(newFiles)
            setFiles(updatedFiles)
            onChange?.(updatedFiles, files)
        }
    }

    const removeFile = (fileIndex) => {
        const deletedFileList = files.filter((_, index) => index !== fileIndex)
        setFiles(deletedFileList)
        onFileRemove?.(deletedFileList)
    }

    // 기존 파일 삭제 처리
    const removeExistingFile = (fileId) => {
        onExistingFileRemove?.(fileId)
    }

    const triggerUpload = (e) => {
        if (!disabled) {
            fileInputField.current?.click()
        }
        e.stopPropagation()
    }

    const renderChildren = () => {
        if (!draggable && !children) {
            return (
                <Button disabled={disabled} onClick={(e) => e.preventDefault()}>
                    Upload
                </Button>
            )
        }

        if (draggable && !children) {
            return <span>Choose a file or drag and drop here</span>
        }

        return children
    }

    const handleDragLeave = useCallback(() => {
        if (draggable) {
            setDragOver(false)
        }
    }, [draggable])

    const handleDragOver = useCallback(() => {
        if (draggable && !disabled) {
            setDragOver(true)
        }
    }, [draggable, disabled])

    const handleDrop = useCallback(() => {
        if (draggable) {
            setDragOver(false)
        }
    }, [draggable])

    const draggableProp = {
        onDragLeave: handleDragLeave,
        onDragOver: handleDragOver,
        onDrop: handleDrop,
    }

    const draggableEventFeedbackClass = `border-primary`

    const uploadClass = classNames(
        'upload',
        draggable && `upload-draggable`,
        draggable && !disabled && `hover:${draggableEventFeedbackClass}`,
        draggable && disabled && 'disabled',
        dragOver && draggableEventFeedbackClass,
        className,
    )

    const uploadInputClass = classNames(
        'upload-input',
        draggable && `draggable`,
    )

    return (
        <>
            <div
                ref={ref}
                className={uploadClass}
                {...(draggable ? draggableProp : { onClick: triggerUpload })}
                {...rest}
            >
                <input
                    ref={fileInputField}
                    className={uploadInputClass}
                    type="file"
                    disabled={disabled}
                    multiple={multiple}
                    accept={accept}
                    title=""
                    value=""
                    onChange={onNewFileUpload}
                    {...rest}
                ></input>
                {renderChildren()}
            </div>
            {tip}
            {showList && (
                <div className={classNames('upload-file-list', fileListClass)}>
                    {/* 기존 파일 목록 렌더링 */}
                    {existingFiles.map((file) => (
                        <FileItem
                            key={`existing_${file.fileId}`}
                            file={{
                                name: file.orgFileName,
                                size: file.fileSize || 0,
                                type: file.fileSeCd || '',
                                isExisting: true, // 기존 파일임을 표시
                                ...file
                            }}
                            className={fileItemClass}
                            isExisting={true}
                        >
                            <CloseButton
                                className="upload-file-remove"
                                onClick={() => removeExistingFile(file.fileId)}
                            />
                        </FileItem>
                    ))}
                    
                    {/* 새로 업로드된 파일 목록 렌더링 */}
                    {files.map((file, index) => (
                        <FileItem
                            key={`new_${file.name}_${index}`}
                            file={file}
                            className={fileItemClass}
                        >
                            <CloseButton
                                className="upload-file-remove"
                                onClick={() => removeFile(index)}
                            />
                        </FileItem>
                    ))}
                </div>
            )}
        </>
    )
}

export default Upload