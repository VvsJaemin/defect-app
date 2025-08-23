package com.group.defectapp.dto.user;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchCondition {

    private String userId;

    private String userName;

    private String userSeCd;
}