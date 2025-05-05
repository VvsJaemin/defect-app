package com.group.defectapp.dto.project;

import com.group.defectapp.domain.project.Project;
import com.group.defectapp.domain.project.ProjectAssignUser;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProjectResponseDto {

    private String projectId;
    private String projectName;
    private String urlInfo;
    private String customerName;
    private String statusCode;
    private String etcInfo;
    private String useYn;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // 할당된 사용자 목록
    private Set<String> assignedUsers;


}