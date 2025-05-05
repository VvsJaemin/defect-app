package com.group.defectapp.service.project;


import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.project.ProjectRequestDto;
import com.group.defectapp.dto.project.ProjectResponseDto;
import com.group.defectapp.dto.project.ProjectSearchCondition;
import com.group.defectapp.service.cmCode.CommonCodeService;
import com.group.defectapp.service.user.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@SpringBootTest
public class ProjectServiceTests {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Autowired
    private CommonCodeService commonCodeService;


    private static final String TEST_ASSIGNEE = "jm0820@groovysoft.co.kr";
    private static final String TEST_STATUS_CODE = "TEST";


    @Test
    public void saveProject_ShouldPersistProject() {
        // 사용자 찾기
        User user = userServiceImpl.findByUserId(TEST_ASSIGNEE);

        // 공통 코드 찾기
        CommonCode commonCode = commonCodeService.findBySeCode(TEST_STATUS_CODE);
        
        // 할당된 사용자 ID 설정
        Set<String> assignedUserIds = new HashSet<>(List.of(
                TEST_ASSIGNEE,
                "qa2@test.co.kr",
                "DM1@test.co.kr",
                "DM2@test.co.kr"
        ));
        
        // DTO 생성
        ProjectRequestDto requestDto = ProjectRequestDto.builder()
                .projectName("테스트 프로젝트")
                .urlInfo("http://test.example.com")
                .customerName("테스트 고객사")
                .statusCode(commonCode.getSeCode())
                .etcInfo("테스트용 프로젝트")
                .useYn("Y")
                .assigneeId(user.getUserId())
                .createdBy(user.getUserId())
                .updatedBy(user.getUserId())
                .projAssignedUsers(assignedUserIds)
                .build();
        
        // 프로젝트 저장
        projectService.saveProject(requestDto);
    }

    @Test
    public void getProjectList() {

        PageRequestDto pageRequestDto = PageRequestDto.builder()
                .page(0)
                .pageSize(10)
                .build();

        ProjectSearchCondition condition = ProjectSearchCondition.builder()
                .searchType("project_id")
                .searchText("PROJ000009")
                .build();

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("searchType", condition.getSearchType());
        paramMap.put("searchText", condition.getSearchText());

        Page<ProjectResponseDto> projectsList = projectService.getProjectsList(pageRequestDto, paramMap);

        System.out.println("projectsList.getContent() = " + projectsList.getContent());
    }

    @Test
    @Transactional
    public void getReadProject() {
        String projectId = "PROJ000009";

        ProjectResponseDto projectResponseDto = projectService.readProject(projectId);

        System.out.println("projectResponseDto = " + projectResponseDto);
    }
    
    @Test
    public void updateProject() {
        // 사용자 찾기
        User user = userServiceImpl.findByUserId(TEST_ASSIGNEE);

        // 공통 코드 찾기
        CommonCode commonCode = commonCodeService.findBySeCode(TEST_STATUS_CODE);
        
        // 할당된 사용자 ID 설정
        Set<String> assignedUserIds = new HashSet<>(List.of(
                TEST_ASSIGNEE,
                "qa2@test.co.kr",
                "DM2@test.co.kr"
        ));

        String projectId = "PROJ000009";

        ProjectRequestDto requestDto = ProjectRequestDto.builder()
                .projectId(projectId)
                .projectName("테스트 프로젝트2")
                .urlInfo("http://test.example.com")
                .customerName("테스트 고객사2")
                .statusCode(commonCode.getSeCode())
                .etcInfo("테스트용 프로젝트3")
                .useYn("Y")
                .assigneeId(user.getUserId())
                .createdBy(user.getUserId())
                .updatedBy(user.getUserId())
                .projAssignedUsers(assignedUserIds)
                .build();
    
        // 프로젝트 업데이트
        projectService.updateProject(requestDto);
    }

    @Test
    public void deleteProject() {
        String projectId = "PROJ000009";
        projectService.deleteProject(projectId);
    }
}