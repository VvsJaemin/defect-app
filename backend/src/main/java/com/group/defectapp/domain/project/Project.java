package com.group.defectapp.domain.project;

import com.group.defectapp.domain.cmCode.BaseEntity;
import com.group.defectapp.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_project_m")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Project extends BaseEntity {

    @Id
    @Column(name = "project_id", nullable = false, length = 48)
    private String projectId;

    @Column(name = "project_nm", length = 256)
    private String projectName;

    @Column(name = "url_info", length = 512)
    private String urlInfo;

    @Column(name = "customer_nm", length = 256)
    private String customerName;

    @Column(name = "status_cd", length = 24)
    private String statusCode;

    @Column(name = "etc_info", length = 512)
    private String etcInfo;

    @Column(name = "use_yn", length = 1)
    private String useYn;

    @Column(name = "first_reg_id", length = 48)
    private String createdBy;

    @Column(name = "fnl_udt_id", length = 48)
    private String updatedBy;

    @ManyToOne
    @JoinColumn(name = "assign_user_id")
    private User assignee;


    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "tb_project_assign_m",
            joinColumns = @JoinColumn(name = "project_id")
            // 유니크 제약조건 명시하지 않음 (테이블에 이미 정의되어 있음)
    )
    @Column(name = "assign_user_id", length = 48)  // 컬럼명 수정
    @Builder.Default
    private Set<String> projAssignedUsers = new HashSet<>();



    public void updateProjectInfo(String projectName, String urlInfo, String customerName,
                                  String statusCode, String etcInfo, String useYn, String updatedBy) {
        this.projectName = projectName;
        this.urlInfo = urlInfo;
        this.customerName = customerName;
        this.statusCode = statusCode;
        this.etcInfo = etcInfo;
        this.useYn = useYn;
        this.updatedBy = updatedBy;
    }

    public void addProjAssignedUser(ProjectAssignUser assignUser) {
        this.projAssignedUsers.add(assignUser.getUserId());
    }

}