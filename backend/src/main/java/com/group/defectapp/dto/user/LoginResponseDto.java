package com.group.defectapp.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {

    private String accessToken;
    private String refreshToken;
    private String userId;
    private String userName;
    private String userSeCd;
    private List<String> authorities;

}
