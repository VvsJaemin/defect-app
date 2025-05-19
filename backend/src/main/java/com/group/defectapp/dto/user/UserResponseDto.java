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
    private LocalDateTime firstRegDtm; // 필드 이름이 일치하지 않을 수 있음
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