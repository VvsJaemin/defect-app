package com.group.defectapp.dto.user;

import com.group.defectapp.domain.user.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserListDto {

    private String userId;
    private String userName;
    private String userSeCd;
    private LocalDateTime lastLoginAt;
    private LocalDateTime firstRegDtm;

}