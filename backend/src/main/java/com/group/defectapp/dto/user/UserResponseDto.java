package com.group.defectapp.dto.user;

import com.group.defectapp.domain.user.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserResponseDto {

    private String userId;

    private String userName;

    private String userSeCd;

    private String userSeNm;

    private LocalDateTime lastLoginAt;

    private LocalDateTime firstRegDtm;

    private LocalDateTime fnlUdtDtm;

    public UserResponseDto(User user) {
        this.userId = user.getUserId();
        this.userName = user.getUserName();
        this.userSeCd = user.getUserSeCd();
        this.lastLoginAt = user.getLastLoginAt();
        this.firstRegDtm = user.getCreatedAt();
        this.fnlUdtDtm = user.getUpdatedAt();
    }
}