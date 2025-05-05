package com.group.defectapp.dto.defectlog;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefectLogListDto {

    private Integer logSeq;
    private String defectId;
    private String logTitle;
    private String logCt;
    private String statusCd;
    private LocalDateTime firstRegDtm;
    private String firstRegId;

}