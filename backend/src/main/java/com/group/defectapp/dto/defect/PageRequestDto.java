package com.group.defectapp.dto.defect;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "페이징 및 정렬 정보 DTO")
public class PageRequestDto {

    @Min(1)
    @Builder.Default
    @Schema(description = "페이지 번호 (1부터 시작)", example = "1")
    private Integer pageIndex = 1;

    @Min(10)
    @Max(100)
    @Builder.Default
    @Schema(description = "페이지 크기 (최소 10, 최대 100)", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "정렬 기준 필드명", example = "createdAt")
    private String sortKey;

    @Schema(description = "정렬 순서 (asc 또는 desc)", example = "desc")
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
