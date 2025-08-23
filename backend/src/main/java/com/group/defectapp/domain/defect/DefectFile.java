package com.group.defectapp.domain.defect;


import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalDateTime;

@Embeddable
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class DefectFile implements Comparable<DefectFile> {

    private int idx;

    private String org_file_name;

    private String sys_file_name;

    private String file_path;

    private String file_se_cd;

    private String first_reg_id;

    private LocalDateTime first_reg_dtm;



    @Override
    public int compareTo(DefectFile o) {
        return this.idx - o.idx;
    }

}
