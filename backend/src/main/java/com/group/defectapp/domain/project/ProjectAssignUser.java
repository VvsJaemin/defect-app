package com.group.defectapp.domain.project;

import com.group.defectapp.domain.cmCode.BaseEntity;
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
public class ProjectAssignUser {

    @Column(name = "project_id", length = 24)
    private String projectId;

    @Column(name = "assign_user_id", length = 48)
    private String userId;
}
