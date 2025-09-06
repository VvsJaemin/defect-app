import { useState, useEffect } from 'react'
import Dialog from '@/components/ui/Dialog'
import Loading from '@/components/shared/Loading'
import DefectSection from '@/views/concepts/defects/DefectDetails/DefectSection.jsx'
import { apiGetDefectRead } from '@/services/DefectService.js'
import isEmpty from 'lodash/isEmpty'

const DefectDetailsModal = ({ isOpen, onClose, defectId }) => {
    const [data, setData] = useState(null)
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState(null)

    useEffect(() => {
        if (isOpen && defectId) {
            fetchDefectDetails()
        }
    }, [isOpen, defectId])

    const fetchDefectDetails = async () => {
        setIsLoading(true)
        setError(null)
        try {
            const result = await apiGetDefectRead({ defectId })
            setData(result)
        } catch (err) {
            setError(err)
        } finally {
            setIsLoading(false)
        }
    }

    const handleClose = () => {
        setData(null)
        setError(null)
        onClose()
    }

    if (!isOpen) return null

    return (
        <Dialog
            isOpen={isOpen}
            onClose={handleClose}
            onRequestClose={handleClose}
            shouldCloseOnOverlayClick={true}
            shouldCloseOnEsc={true}
            width={1400}
            height="90vh"
        >
            <div className="h-full flex flex-col" style={{ maxHeight: '90vh' }}>
                <div className="flex-shrink-0 px-6 py-4 border-b border-gray-200 bg-white">
                    <h2 className="text-xl font-bold text-gray-900">결함 상세보기</h2>
                </div>

                <div
                    className="flex-1 px-6 py-4"
                    style={{
                        overflowY: 'scroll',
                        overflowX: 'hidden',
                        maxHeight: 'calc(90vh - 100px)',
                        scrollbarWidth: 'thin',
                        scrollbarColor: '#cbd5e1 #f1f5f9'
                    }}
                >
                    {error ? (
                        <div className="text-center text-red-500 py-10">
                            결함 정보를 불러오는 중 오류가 발생했습니다. 다시 시도해주세요.
                        </div>
                    ) : (
                        <Loading loading={isLoading} spinnerClass="my-10">
                            {!isEmpty(data) && (
                                <DefectSection data={data} isModal={true} onCloseModal={handleClose} />
                            )}
                        </Loading>
                    )}
                </div>
            </div>
        </Dialog>
    )
}

export default DefectDetailsModal