package com.group.defectapp.service.project;

import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.common.PageRequestDto;
import com.group.defectapp.dto.project.ProjectRequestDto;
import com.group.defectapp.dto.project.ProjectResponseDto;
import com.group.defectapp.service.cmCode.CommonCodeService;
import com.group.defectapp.service.user.UserServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProjectServiceTests {

    @Autowired private ProjectService projectService;
    @Autowired private UserServiceImpl userServiceImpl;
    @Autowired private CommonCodeService commonCodeService;

    private static final String TEST_ASSIGNEE = "jm0820@test.co.kr";
    private static final String TEST_STATUS_CODE = "TEST";
    private static final String TEST_PROJECT_ID = "PROJ000009";

    private User user;
    private CommonCode commonCode;

    @BeforeAll
    void setup() {
        user = userServiceImpl.findByUserId(TEST_ASSIGNEE);
        commonCode = commonCodeService.findBySeCode(TEST_STATUS_CODE);
    }

    @Test
    @Order(1)
    @DisplayName("프로젝트를 저장하면 성공적으로 생성되어야 한다")
    void save_project_should_persist() {
        Set<String> assignedUserIds = Set.of(
                TEST_ASSIGNEE, "qa2@test.co.kr", "DM1@test.co.kr", "DM2@test.co.kr"
        );

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

        projectService.saveProject(requestDto);
    }

    @Test
    @Order(2)
    @DisplayName("조건에 맞는 프로젝트 리스트를 조회할 수 있어야 한다")
    void search_project_list_should_return_result() {
        PageRequestDto pageRequestDto = PageRequestDto.builder()
                .pageIndex(0)
                .pageSize(10)
                .build();

        Map<String, Object> paramMap = Map.of(
                "searchType", "project_id",
                "searchText", TEST_PROJECT_ID
        );

        Page<ProjectResponseDto> projectsList = projectService.getProjectsList(pageRequestDto, paramMap);
        assertNotNull(projectsList);
        System.out.println("조회 결과: " + projectsList.getContent());
    }

    @Test
    @Order(3)
    @DisplayName("프로젝트 ID로 상세 정보를 조회할 수 있어야 한다")
    @Transactional
    void read_project_should_return_detail() {
        ProjectResponseDto response = projectService.readProject(TEST_PROJECT_ID);
        assertNotNull(response);
        System.out.println("상세 정보: " + response);
    }

    @Test
    @Order(4)
    @DisplayName("프로젝트를 수정하면 변경 사항이 반영되어야 한다")
    void update_project_should_reflect_changes() {
        Set<String> assignedUserIds = Set.of(
                TEST_ASSIGNEE, "qa2@test.co.kr", "DM2@test.co.kr"
        );

        ProjectRequestDto requestDto = ProjectRequestDto.builder()
                .projectId(TEST_PROJECT_ID)
                .projectName("테스트 프로젝트 수정본")
                .urlInfo("http://test.example.com")
                .customerName("수정된 고객사")
                .statusCode(commonCode.getSeCode())
                .etcInfo("수정된 프로젝트 설명")
                .useYn("Y")
                .assigneeId(user.getUserId())
                .createdBy(user.getUserId())
                .updatedBy(user.getUserId())
                .projAssignedUsers(assignedUserIds)
                .build();

        projectService.updateProject(requestDto);
    }

    @Test
    @Order(5)
    @DisplayName("프로젝트 ID로 삭제 요청 시 성공적으로 삭제되어야 한다")
    void delete_project_should_remove_it() {
        projectService.deleteProject(TEST_PROJECT_ID);
    }
}
