package com.group.defectapp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(title = "로그인 요청 DTO", description = "사용자 로그인을 위한 인증 정보")
public class LoginRequestDto {

    @Schema(
            description = "사용자 ID",
            example = "USER001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String userId;

    @Schema(
            description = "사용자 비밀번호",
            example = "password123!",
            requiredMode = Schema.RequiredMode.REQUIRED,
            format = "password"
    )
    private String password;
}