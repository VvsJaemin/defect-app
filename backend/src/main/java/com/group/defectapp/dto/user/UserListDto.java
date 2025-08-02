package com.group.defectapp.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(
        title = "사용자 목록 DTO",
        description = "사용자 엔티티 기반의 리스트 데이터 모델"
)
public class UserListDto implements Serializable {

    @Schema(
            description = "사용자 계정",
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
            example = "ADMIN"
    )
    private String userSeCd;

    @Schema(
            description = "비밀번호 오류 횟수",
            example = "1"
    )
    private int pwdFailCnt;

    @Schema(
            description = "마지막 로그인 시간 (yyyy-MM-dd HH:mm:ss)",
            example = "2024-08-01 12:30:45"
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @Schema(
            description = "계정 생성 시간 (yyyy-MM-dd HH:mm:ss)",
            example = "2024-01-05 09:40:00"
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}