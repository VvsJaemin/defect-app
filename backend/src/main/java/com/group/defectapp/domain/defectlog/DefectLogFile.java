package com.group.defectapp.domain.defectlog;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@Schema(
        title = "결함 로그 첨부파일",
        description = "결함 로그에 첨부된 파일 정보"
)
public class DefectLogFile implements Comparable<DefectLogFile> {

    @Schema(
            description = "파일 순서 인덱스",
            example = "0"
    )
    private int idx;

    @Schema(
            description = "원본 파일명",
            example = "screenshot.png"
    )
    private String org_file_name;

    @Schema(
            description = "시스템 저장 파일명",
            example = "sys_20240801_001.png"
    )
    private String sys_file_name;

    @Schema(
            description = "파일 저장 경로",
            example = "/uploads/defectlog/2024/08/01/"
    )
    private String file_path;

    @Schema(
            description = "파일 구분 코드",
            example = "IMG"
    )
    private String file_se_cd;

    @Schema(
            description = "파일 등록자 ID",
            example = "user01"
    )
    private String first_reg_id;

    @Schema(
            description = "파일 등록 시간 (yyyy-MM-dd HH:mm:ss)",
            example = "2024-08-01 14:30:00"
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime first_reg_dtm;

    @Schema(
            description = "연관된 결함 ID",
            example = "DEFECT-2024-001"
    )
    @Column(name = "log_defect_id", length = 48)
    private String defectId;

    @Override
    public int compareTo(DefectLogFile o) {
        return this.idx - o.idx;
    }
}