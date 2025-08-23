package com.group.defectapp.dto.user;

import com.group.defectapp.annotation.ConditionalEmail;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {

    @NotBlank(message = "사용자 ID는 필수입니다.")
    @ConditionalEmail
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
    private String userId;

    @NotBlank(message = "사용자 이름은 필수입니다")
    @Size(min = 2, max = 50, message = "사용자 이름은 2자 이상 50자 이하로 입력해주세요")
    private String userName;

    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "비밀번호는 영문 / 숫자 / 특수문자를 각각 하나 이상 포함해야 합니다."
    )
    private String password;

    @NotBlank(message = "사용자 구분 코드는 필수입니다.")
    @Pattern(
            regexp = "^(CU|DM|DP|MG|QA)$",
            message = "사용자 구분 코드는 CU / DM / DP / MG / QA 중 하나여야 합니다."
    )
    private String userSeCd;
}