package com.group.defectapp.dto.project;

import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.domain.project.ProjectAssignUser;
import com.group.defectapp.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRequestDto {

    private String projectId;
    private String projectName;
    private String urlInfo;
    private String customerName;
    private String statusCode;
    private String etcInfo;
    private String useYn;
    private String assigneeId;

    // 할당된 사용자 ID 목록 추가
    private Set<String> projAssignedUsers;

    // 생성자/수정자 정보 추가
    private String createdBy;
    private String updatedBy;

    public Project toEntity(User assignee, String newProjectId, CommonCode commonCode) {
        return Project.builder()
                .projectId(newProjectId)
                .projectName(projectName)
                .urlInfo(urlInfo)
                .customerName(customerName)
                .statusCode(commonCode.getSeCode())
                .etcInfo(etcInfo)
                .useYn(useYn != null ? useYn : "Y") // 기본값 설정
                .assignee(assignee)
                .createdBy(createdBy != null ? createdBy : (assignee != null ? assignee.getUserId() : null))
                .updatedBy(createdBy != null ? createdBy : (assignee != null ? assignee.getUserId() : null))
                .projAssignedUsers(projAssignedUsers)
                .build();
    }

}
