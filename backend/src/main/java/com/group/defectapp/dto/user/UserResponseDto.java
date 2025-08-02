package com.group.defectapp.dto.user;

import com.group.defectapp.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(
        title = "사용자 응답 DTO",
        description = "사용자 상세 정보 응답 데이터 모델"
)
public class UserResponseDto {

    @Schema(
            description = "사용자 계정(아이디)",
            example = "user01"
    )
    private String userId;

    @Schema(
            description = "사용자 이름",
            example = "홍길동"
    )
    private String userName;

    @Schema(
            description = "사용자 구분 코드(예: 'ADMIN', 'USER' 등)",
            example = "USER",
            allowableValues = {"ADMIN", "USER", "GUEST"}
    )
    private String userSeCd;

    @Schema(
            description = "사용자 구분명(예: 일반사용자·관리자 등)",
            example = "일반사용자"
    )
    private String userSeNm;

    @Schema(
            description = "마지막 로그인 일시 (yyyy-MM-dd HH:mm:ss)",
            example = "2024-08-01 12:30:45"
    )
    private LocalDateTime lastLoginAt;

    @Schema(
            description = "최초 등록 일시 (yyyy-MM-dd HH:mm:ss)",
            example = "2024-01-05 09:40:00"
    )
    private LocalDateTime firstRegDtm;

    @Schema(
            description = "최종 수정 일시 (yyyy-MM-dd HH:mm:ss)",
            example = "2024-07-20 14:30:00"
    )
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