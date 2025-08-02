package com.group.defectapp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        title = "사용자 생성/수정 요청 DTO",
        description = "신규 사용자 생성 또는 정보 수정을 위한 요청 데이터 모델"
)
public class UserRequestDto {

    @Schema(
            description = "사용자 계정(아이디)",
            example = "user01",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String userId;

    @Schema(
            description = "사용자 이름",
            example = "홍길동",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String userName;

    @Schema(
            description = "비밀번호(신규 생성 시 필수)",
            example = "qwer!234",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;

    @Schema(
            description = "사용자 구분 코드(예: 'ADMIN', 'USER' 등)",
            example = "USER",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"ADMIN", "USER", "GUEST"}
    )
    private String userSeCd;
}