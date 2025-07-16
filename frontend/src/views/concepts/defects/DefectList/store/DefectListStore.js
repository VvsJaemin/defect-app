import { create } from 'zustand'

export const initialTableData = {
    pageIndex: 1,
    pageSize: 10,
}

export const initialFilterData = {
    defectId: '',
    defectTitle: '',
    projectId: '',
    statusCode: '',
    assigneeId: '',

}

const initialState = {
    tableData: initialTableData,
    filterData: initialFilterData,
    selectedDefect: null,
}

export const useDefectListStore = create((set) => ({
    ...initialState,
    setFilterData: (payload) => set(() => ({ filterData: payload })),
    setTableData: (payload) => set(() => ({ tableData: payload })),
    setSelectedDefect: (checked, row) =>
        set((state) => {
            const currentSelected = state.selectedDefect;

            if (checked) {
                // 새로운 항목 선택 (기존 선택은 자동으로 해제됨)
                return { selectedDefect: row };
            } else {
                // 현재 선택된 항목과 같은 항목을 체크 해제하는 경우에만 null로 설정
                if (currentSelected && currentSelected.defectId === row.defectId) {
                    return { selectedDefect: null };
                }
            }

            return {}; // 아무 변경도 없으면 빈 객체 반환 (상태 유지)
        }),
    setSelectAllDefect: () =>
        set(() => {
            return {
                selectedDefect: null,       // 선택 해제
                isSelectDisabled: true,     // 클릭 비활성화
            };
        }),

}))