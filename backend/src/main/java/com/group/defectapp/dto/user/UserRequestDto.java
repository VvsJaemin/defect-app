package com.group.defectapp.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {

    private String userId;

    private String userName;

    private String password;

    private String userSeCd;
}