package com.group.defectapp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        title = "사용자 검색 조건 DTO",
        description = "사용자 목록을 조건별로 조회할 때 사용하는 검색 조건 데이터"
)
public class UserSearchCondition {

    @Schema(
            description = "사용자 계정(아이디) 검색",
            example = "user01",
            nullable = true
    )
    private String userId;

    @Schema(
            description = "사용자 이름 검색 (부분일치 가능)",
            example = "홍길동",
            nullable = true
    )
    private String userName;

    @Schema(
            description = "사용자 구분 코드 검색 (예: 'ADMIN', 'USER', 'GUEST')",
            example = "USER",
            allowableValues = {"ADMIN", "USER", "GUEST"},
            nullable = true
    )
    private String userSeCd;
}