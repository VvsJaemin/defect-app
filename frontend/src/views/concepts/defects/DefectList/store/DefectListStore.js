import { create } from 'zustand'

export const initialTableData = {
    pageIndex: 1,
    pageSize: 10,
}

export const initialFilterData = {
    defectId: null,
    defectTitle: null,
}

const initialState = {
    tableData: initialTableData,
    filterData: initialFilterData,
    selectedDefect: [],
}

export const useDefectListStore = create((set) => ({
    ...initialState,
    setFilterData: (payload) => set(() => ({ filterData: payload })),
    setTableData: (payload) => set(() => ({ tableData: payload })),
    setSelectedDefect: (checked, row) =>
        set((state) => {
            const prevData = state.selectedDefect;
            const isAlreadySelected = prevData.some(
                (defect) => defect.defectId == row.defectId
            );

            if (checked) {
                if (!isAlreadySelected) {
                    return { selectedDefect: [...prevData, row] };
                }
            } else {
                return {
                    selectedDefect: prevData.filter(
                        (defect) => defect.defectId != row.defectId
                    ),
                };
            }

            return {}; // 아무 변경도 없으면 빈 객체 반환 (상태 유지)
        }),
    setSelectAllDefect: (rows) =>
        set(() => (
            { selectedDefect: rows })
        ), // 전체 선택 시 모든 항목을 추가
}))