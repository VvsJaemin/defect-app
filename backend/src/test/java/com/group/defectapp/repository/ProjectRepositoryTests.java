package com.group.defectapp.repository;

import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.domain.project.ProjectAssignUser;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.project.ProjectResponseDto;
import com.group.defectapp.dto.project.ProjectSearchCondition;
import com.group.defectapp.repository.cmCode.CommonCodeRepository;
import com.group.defectapp.repository.project.ProjectRepository;
import com.group.defectapp.repository.user.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProjectRepositoryTests {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    private static String testProjectId;
    private static final String TEST_ASSIGNEE = "jm0820@groovysoft.co.kr";
    private static final String TEST_STATUS_CODE = "TEST";

    @BeforeEach
    void setUp() {
        // 테스트에 필요한 데이터 확인
        assertTrue(userRepository.findByUserId(TEST_ASSIGNEE).isPresent(),
                "테스트 사용자가 DB에 존재해야 합니다: " + TEST_ASSIGNEE);
        assertTrue(commonCodeRepository.findBySeCode(TEST_STATUS_CODE).isPresent(),
                "테스트 상태 코드가 DB에 존재해야 합니다: " + TEST_STATUS_CODE);
    }

    @Test
    @Order(1)
    @Transactional
    @DisplayName("프로젝트 생성 테스트")
    public void createProject_ShouldPersistProject() {
        // Given
        User user = userRepository.findByUserId(TEST_ASSIGNEE)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다: " + TEST_ASSIGNEE));

        CommonCode commonCode = commonCodeRepository.findBySeCode(TEST_STATUS_CODE)
                .orElseThrow(() -> new IllegalArgumentException("해당 상태코드를 찾을 수 없습니다: " + TEST_STATUS_CODE));

        Set<String> assignedUserIds = new HashSet<>(List.of(
                TEST_ASSIGNEE,
                "qa2@test.co.kr",
                "DM1@test.co.kr",
                "DM2@test.co.kr"
        ));

        // 새 프로젝트 ID 생성
        testProjectId = projectRepository.generateProjectIdUsingSequence();
        assertNotNull(testProjectId, "프로젝트 ID가 생성되어야 합니다");

        // When
        Project project = Project.builder()
                .projectId(testProjectId)
                .projectName("테스트 프로젝트")
                .urlInfo("http://test.example.com")
                .customerName("테스트 고객사")
                .statusCode(commonCode.getSeCode())
                .etcInfo("테스트용 프로젝트")
                .useYn("Y")
                .assignee(user)
                .createdBy(user.getUserId())
                .updatedBy(user.getUserId())
                .projAssignedUsers(assignedUserIds)
                .build();

        Project savedProject = projectRepository.save(project);

        // Then
        assertNotNull(savedProject, "프로젝트가 저장되어야 합니다");
        assertEquals(testProjectId, savedProject.getProjectId(), "저장된 프로젝트 ID가 일치해야 합니다");
        assertEquals("테스트 프로젝트", savedProject.getProjectName(), "프로젝트 이름이 일치해야 합니다");
        assertEquals(4, savedProject.getProjAssignedUsers().size(), "할당된 사용자 수가 일치해야 합니다");
    }

    @Test
    @Order(2)
    @DisplayName("기본 페이징으로 프로젝트 목록 조회 테스트")
    public void listProjectsWithPaging_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        ProjectSearchCondition condition = ProjectSearchCondition.builder()
                .searchType("project_id")
                .searchText("PROJ000009")
                .build();

        // When
        Page<ProjectResponseDto> projectPage = projectRepository.list(pageable, condition);


        System.out.println("projectPage = " + projectPage.getContent());

        // Then
        assertNotNull(projectPage, "프로젝트 페이지가 반환되어야 합니다");
        assertFalse(projectPage.isEmpty(), "프로젝트 목록이 비어있지 않아야 합니다");
        assertTrue(projectPage.getTotalElements() > 0, "전체 항목 수가 0보다 커야 합니다");

        // 페이징 정보 확인
        assertEquals(0, projectPage.getNumber(), "현재 페이지 번호가 0이어야 합니다");
        assertTrue(projectPage.getSize() >= projectPage.getContent().size(), "페이지 크기가 올바르게 설정되어야 합니다");

    }

    @Test
    @Order(3)
    @DisplayName("프로젝트 조회 테스트")
    public void readProject_ShouldRetrieveProject() {
        // Given
        // 테스트 프로젝트가 생성되었다고 가정
        testProjectId = "PROJ000009";

        // When
        Optional<Project> foundProject = projectRepository.findByProjectId(testProjectId);

        // Then
        assertTrue(foundProject.isPresent(), "프로젝트가 조회되어야 합니다");
        Project project = foundProject.get();
        assertEquals("수정된 프로젝트명", project.getProjectName(), "프로젝트 이름이 일치해야 합니다");
        assertEquals("테스트 운영 고객사", project.getCustomerName(), "고객사 이름이 일치해야 합니다");
        assertEquals(TEST_STATUS_CODE, project.getStatusCode(), "상태 코드가 일치해야 합니다");
        assertFalse(project.getProjAssignedUsers().isEmpty(), "할당된 사용자가 있어야 합니다");
    }

    @Test
    @Order(4)
    @Transactional
    @DisplayName("프로젝트 업데이트 테스트")
    public void updateProject_ShouldUpdateProject() {
        // Given
        Project foundProject = projectRepository.findByProjectId(testProjectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + testProjectId));

        String updatedName = "업데이트된 프로젝트";
        String updatedUrl = "http://updated.example.com";
        String updatedCustomer = "업데이트된 고객사";

        // 할당자 목록 업데이트
        Set<String> updatedUserIds = new HashSet<>();
        updatedUserIds.add(TEST_ASSIGNEE);
        updatedUserIds.add("DM1@test.co.kr");

        // When
        // 프로젝트 정보 업데이트
        foundProject.updateProjectInfo(
                updatedName,
                updatedUrl,
                updatedCustomer,
                TEST_STATUS_CODE,
                "업데이트된 추가 정보",
                "Y",
                TEST_ASSIGNEE
        );

        // 할당 사용자 목록 업데이트
        foundProject.getProjAssignedUsers().clear();
        for (String userId : updatedUserIds) {
            ProjectAssignUser assignUser = ProjectAssignUser.builder()
                    .projectId(testProjectId)
                    .userId(userId)
                    .build();
            foundProject.addProjAssignedUser(assignUser);
        }

        projectRepository.save(foundProject);

        // Then
        Project updatedProject = projectRepository.findByProjectId(testProjectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다"));

        assertEquals(updatedName, updatedProject.getProjectName(), "프로젝트 이름이 업데이트되어야 합니다");
        assertEquals(updatedUrl, updatedProject.getUrlInfo(), "URL이 업데이트되어야 합니다");
        assertEquals(updatedCustomer, updatedProject.getCustomerName(), "고객사 이름이 업데이트되어야 합니다");
        assertEquals(2, updatedProject.getProjAssignedUsers().size(), "할당된 사용자 수가 업데이트되어야 합니다");
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("프로젝트 삭제 테스트")
    public void deleteProject_ShouldRemoveProject() {
        // Given
        Project project = projectRepository.findByProjectId(testProjectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + testProjectId));

        // When
        projectRepository.delete(project);

        // Then
        Optional<Project> deletedProject = projectRepository.findByProjectId(testProjectId);
        assertFalse(deletedProject.isPresent(), "프로젝트가 삭제되어야 합니다");
    }


}