import { create } from 'zustand'

export const initialTableData = {
    pageIndex: 1,
    pageSize: 10,
}

export const initialFilterData = {
    projectName: null,
    urlInfo: null,
    customerName: null,
    projectState: null,
}

const initialState = {
    tableData: initialTableData,
    filterData: initialFilterData,
    selectedProject: [],
}

export const useProjectListStore = create((set) => ({
    ...initialState,
    setFilterData: (payload) => set(() => ({ filterData: payload })),
    setTableData: (payload) => set(() => ({ tableData: payload })),
    setSelectedProject: (checked, row) =>
        set((state) => {
            const prevData = state.selectedProject;
            const isAlreadySelected = prevData.some(
                (project) => project.projectId == row.projectId
            );

            if (checked) {
                if (!isAlreadySelected) {
                    return { selectedProject: [...prevData, row] };
                }
            } else {
                return {
                    selectedProject: prevData.filter(
                        (project) => project.projectId != project.projectId
                    ),
                };
            }

            return {}; // 아무 변경도 없으면 빈 객체 반환 (상태 유지)
        }),
    setSelectAllProject: (rows) =>
        set(() => (
            { selectedProject: rows })
        ), // 전체 선택 시 모든 고객을 추가
}))
