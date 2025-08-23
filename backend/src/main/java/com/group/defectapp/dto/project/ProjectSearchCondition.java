package com.group.defectapp.dto.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectSearchCondition {

    private String projectName;

    private String customerName;

    private String projectState;

    private String urlInfo;

    // 테스트에서 사용하는 필드들 추가
    private String searchType;

    private String searchText;
}