import { create } from 'zustand'

export const initialTableData = {
    page: 1,
    pageSize: 10,
}

export const initialFilterData = {
    userId: null,
    userName: null,
    userSeCd: null,
}

const initialState = {
    tableData: initialTableData,
    filterData: initialFilterData,
    selectedCustomer: [],
}

export const useCustomerListStore = create((set) => ({
    ...initialState,
    setFilterData: (payload) => set(() => ({ filterData: payload })),
    setTableData: (payload) => set(() => ({ tableData: payload })),
    setSelectedCustomer: (checked, row) =>
        set((state) => {
            const prevData = state.selectedCustomer;
            const isAlreadySelected = prevData.some(
                (customer) => customer.userId == row.userId
            );

            if (checked) {
                if (!isAlreadySelected) {
                    return { selectedCustomer: [...prevData, row] };
                }
            } else {
                return {
                    selectedCustomer: prevData.filter(
                        (customer) => customer.userId != row.userId
                    ),
                };
            }

            return {}; // 아무 변경도 없으면 빈 객체 반환 (상태 유지)
        }),
    setSelectAllCustomer: (rows) =>
        set(() => (
            { selectedCustomer: rows })
        ), // 전체 선택 시 모든 고객을 추가
}))
