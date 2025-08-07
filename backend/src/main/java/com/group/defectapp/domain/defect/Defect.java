package com.group.defectapp.domain.defect;

import com.group.defectapp.domain.cmCode.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.SortedSet;
import java.util.TreeSet;

@Entity
@Table(name = "tb_defect_m")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Defect extends BaseEntity {

    @Id
    @Column(name = "defect_id", nullable = false, length = 48)
    private String defectId;

    @Column(name = "project_id", nullable = false, length = 48)
    private String projectId;

    @Column(name = "assign_user_id")
    private String assignee;

    @Column(name = "status_cd", length = 24)
    private String statusCode;

    @Column(name = "serious_cd", length = 24)
    private String seriousCode;

    @Column(name = "order_cd", length = 24)
    private String orderCode;

    @Column(name = "defect_div_cd", length = 24)
    private String defectDivCode;

    @Column(name = "defect_title", length = 1024)
    private String defectTitle;

    @Column(name = "defect_menu_title", length = 1024)
    private String defectMenuTitle;

    @Column(name = "defect_url_info", length = 256)
    private String defectUrlInfo;

    @Column(name = "defect_ct", length = 4000)
    private String defectContent;

    @Column(name = "defect_etc_ct", length = 4000)
    private String defectEtcContent;

    @Column(name = "first_reg_id", length = 48)
    private String createdBy;

    @Column(name = "fnl_udt_id", length = 48)
    private String updatedBy;

    @Column(name = "open_yn", nullable = false, length = 1)
    private String openYn;


    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "defect_files_m",
            joinColumns = @JoinColumn(name = "defect_id")
    )
    @Builder.Default
    private SortedSet<DefectFile> defectFiles = new TreeSet<>();

    public void addDefectFile(String orgFileName, String sysFileName, String filePath) {
        DefectFile defectFile = DefectFile.builder()
                .idx(defectFiles.size())
                .org_file_name(orgFileName)
                .sys_file_name(sysFileName)
                .file_path(filePath)
                .first_reg_id(createdBy)
                .build();

        defectFiles.add(defectFile);
    }

    public void clearDefectImages() {
        defectFiles.clear();
    }

    public void updateDefect(String defectTitle, String defectContent, String statusCode, String seriousCode, String orderCode, String assignee, String defectDivCode,
                             String defectMenuTitle, String defectUrlInfo, String defectEtcContent, String updatedBy) {
        this.defectTitle = defectTitle;
        this.defectContent = defectContent;
        this.statusCode = statusCode;
        this.seriousCode = seriousCode;
        this.orderCode = orderCode;
        this.assignee = assignee;
        this.defectDivCode = defectDivCode;
        this.defectMenuTitle = defectMenuTitle;
        this.defectUrlInfo = defectUrlInfo;
        this.defectEtcContent = defectEtcContent;
        this.updatedBy = updatedBy;
    }

}