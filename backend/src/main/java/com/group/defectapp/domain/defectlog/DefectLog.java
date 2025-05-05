package com.group.defectapp.domain.defectlog;

import com.group.defectapp.domain.cmCode.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.SortedSet;
import java.util.TreeSet;

@Entity
@Table(name = "tb_defect_log_m")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DefectLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_seq")
    private Integer logSeq;

    @Column(name = "defect_id", nullable = false, length = 48)
    private String defectId;

    @Column(name = "log_title", length = 1024)
    private String logTitle;

    @Column(name = "log_ct", length = 4000)
    private String logCt;

    @Column(name = "status_cd", length = 24)
    private String statusCd;

    @Column(name = "first_reg_id", length = 48)
    private String createdBy;


    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "defect_log_files_m",
            joinColumns = @JoinColumn(name = "log_seq"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"log_seq", "log_defect_id", "idx"})
    )
    @Builder.Default
    private SortedSet<DefectLogFile> defectLogFiles = new TreeSet<>();

    public void addDefectLogFile(String defectId, String orgFileName, String sysFileName, String filePath) {
        DefectLogFile defectLogFile = DefectLogFile.builder()
                .idx(defectLogFiles.size())
                .org_file_name(orgFileName)
                .sys_file_name(sysFileName)
                .file_path(filePath)
                .defectId(defectId)
                .first_reg_id(createdBy)
                .build();

        defectLogFiles.add(defectLogFile);
    }


    public void clearDefectLogFiles() {
        defectLogFiles.clear();
    }
}