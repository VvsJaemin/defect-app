package com.group.defectapp.domain.defectlog;


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
public class DefectLogFile implements Comparable<DefectLogFile> {

    private int idx;
    private String org_file_name;
    private String sys_file_name;
    private String file_path;
    private String file_se_cd;
    private String first_reg_id;
    private LocalDateTime first_reg_dtm;
    @Column(name = "log_defect_id", length = 48)
    private String defectId;


    @Override
    public int compareTo(DefectLogFile o) {
        return this.idx - o.idx;
    }

}
