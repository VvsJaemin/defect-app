package com.group.defectapp.dto.defect;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDto {

    @Min(1)
    @Builder.Default
    private Integer pageIndex = 1;

    @Min(10)
    @Max(100)
    @Builder.Default
    private Integer pageSize = 10;

    private String sortKey;
    private String sortOrder;

    public Pageable getPageable() {
        Sort sort = Sort.unsorted();

        if (sortKey != null && !sortKey.isBlank()) {
            sort = "desc".equalsIgnoreCase(sortOrder)
                    ? Sort.by(sortKey).descending()
                    : Sort.by(sortKey).ascending();
        }

        return PageRequest.of(Math.max(pageIndex - 1, 0), pageSize, sort);
    }
}
