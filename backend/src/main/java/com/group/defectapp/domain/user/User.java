package com.group.defectapp.domain.user;

import com.group.defectapp.domain.cmCode.BaseEntity;
import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.project.Project;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "tb_user_m")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class User extends BaseEntity {
    
    @Id
    @Column(nullable = false, name = "user_id")
    @NotNull
    private String userId;

    @Column(nullable = false)
    @NotNull
    private String password;

    @Column(name = "user_nm")
    private String userName;

    private String userSeCd;

    @Column(columnDefinition = "DATETIME")
    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "assignee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Project> createdProject = new ArrayList<>();

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public void changeUserName(String newUserName) {
        this.userName = newUserName;
    }

    public void changeUserSeCd(String newUserSeCd) {
        this.userSeCd = newUserSeCd;
    }

    public void changeLastLoginAt(LocalDateTime newLastLoginAt) {
        this.lastLoginAt = newLastLoginAt;
    }

    // 스프링 시큐리티에서 사용할 권한 반환
    public List<String> getRoles() {
        if (userSeCd == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList("ROLE_" + userSeCd); // 예: ROLE_USER, ROLE_ADMIN
    }

}
