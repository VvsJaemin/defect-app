package com.group.defectapp.domain.defect;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalDateTime;

@Embeddable
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@Schema(description = "결함 파일 정보")
public class DefectFile implements Comparable<DefectFile> {

    @Schema(description = "첨부파일 정렬 인덱스", example = "1")
    private int idx;

    @Schema(description = "원본 파일명", example = "spec.pdf")
    private String org_file_name;

    @Schema(description = "시스템 파일명", example = "20240803_123456_spec.pdf")
    private String sys_file_name;

    @Schema(description = "파일 저장 경로", example = "/upload/2024/08/03")
    private String file_path;

    @Schema(description = "파일 구분 코드", example = "DEFECT")
    private String file_se_cd;

    @Schema(description = "최초 등록자 ID", example = "admin01")
    private String first_reg_id;

    @Schema(
            description = "최초 등록 일시",
            example = "2024-08-03T14:30:00",
            type = "string",
            format = "date-time"
    )
    private LocalDateTime first_reg_dtm;



    @Override
    public int compareTo(DefectFile o) {
        return this.idx - o.idx;
    }

}
