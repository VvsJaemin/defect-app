package com.group.defectapp.dto.user;

import com.group.defectapp.annotation.ConditionalEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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
            example = "user01@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "사용자 ID는 필수입니다.")
    @ConditionalEmail
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
    private String userId;

    @Schema(
            description = "사용자 이름",
            example = "홍길동",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "사용자 이름은 필수입니다")
    @Size(min = 2, max = 50, message = "사용자 이름은 2자 이상 50자 이하로 입력해주세요")
    private String userName;

    @Schema(
            description = "비밀번호(신규 생성 시 필수, 수정 시 선택)",
            example = "mypass123!",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "비밀번호는 영문 / 숫자 / 특수문자를 각각 하나 이상 포함해야 합니다."
    )
    private String password;

    @Schema(
            description = "사용자 구분 코드",
            example = "DP",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"CU", "DM", "DP", "MG", "QA"}
    )
    @NotBlank(message = "사용자 구분 코드는 필수입니다.")
    @Pattern(
            regexp = "^(CU|DM|DP|MG|QA)$",
            message = "사용자 구분 코드는 CU / DM / DP / MG / QA 중 하나여야 합니다."
    )
    private String userSeCd;
}