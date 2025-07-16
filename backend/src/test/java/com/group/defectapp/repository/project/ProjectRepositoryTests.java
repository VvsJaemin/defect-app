package com.group.defectapp.repository.project;

import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.domain.project.ProjectAssignUser;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.project.ProjectResponseDto;
import com.group.defectapp.dto.project.ProjectSearchCondition;
import com.group.defectapp.repository.cmCode.CommonCodeRepository;
import com.group.defectapp.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProjectRepositoryTests {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    // 테스트 데이터를 중앙화
    private static final String TEST_ASSIGNEE = "jm0820@test.co.kr";
    private static final String TEST_STATUS_CODE = "TEST";
    private static final String EXISTING_PROJECT_ID = "PROJ000009";

    private String testProjectId;
    private User testUser;
    private CommonCode testStatusCode;

    @BeforeAll
    void setUpTestData() {
        // 테스트에 필요한 데이터 확인 및 초기화
        assertTrue(userRepository.findByUserId(TEST_ASSIGNEE).isPresent(),
                "테스트 사용자가 DB에 존재해야 합니다: " + TEST_ASSIGNEE);
        assertTrue(commonCodeRepository.findBySeCode(TEST_STATUS_CODE).isPresent(),
                "테스트 상태 코드가 DB에 존재해야 합니다: " + TEST_STATUS_CODE);

        testUser = userRepository.findByUserId(TEST_ASSIGNEE)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다: " + TEST_ASSIGNEE));

        testStatusCode = commonCodeRepository.findBySeCode(TEST_STATUS_CODE)
                .orElseThrow(() -> new IllegalArgumentException("해당 상태코드를 찾을 수 없습니다: " + TEST_STATUS_CODE));
    }

    @Test
    @DisplayName("새 프로젝트를 생성하면 DB에 저장되어야 한다")
    @Transactional
    void createProject_ShouldPersistProject() {
        // Given: 프로젝트 생성 준비
        Set<String> assignedUserIds = createTestUserSet();
        testProjectId = projectRepository.generateProjectIdUsingSequence();
        assertNotNull(testProjectId, "프로젝트 ID가 생성되어야 합니다");

        // When: 프로젝트 생성 및 저장
        Project project = createTestProject(testProjectId, assignedUserIds);
        Project savedProject = projectRepository.save(project);

        // Then: 저장 결과 검증
        assertNotNull(savedProject, "프로젝트가 저장되어야 합니다");
        assertEquals(testProjectId, savedProject.getProjectId(), "저장된 프로젝트 ID가 일치해야 합니다");
        assertEquals("테스트 프로젝트", savedProject.getProjectName(), "프로젝트 이름이 일치해야 합니다");
        assertEquals(assignedUserIds.size(), savedProject.getProjAssignedUsers().size(),
                "할당된 사용자 수가 일치해야 합니다");
    }

    @Test
    @DisplayName("검색 조건에 맞는 프로젝트가 페이징되어 조회되어야 한다")
    void listProjectsWithPaging_ShouldReturnPagedResults() {
        // Given: 페이징 및 검색 조건 설정
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        ProjectSearchCondition condition = ProjectSearchCondition.builder()
                .searchType("project_id")
                .searchText(EXISTING_PROJECT_ID)
                .build();

        // When: 프로젝트 조회
        Page<ProjectResponseDto> projectPage = projectRepository.list(pageable, condition);

        // Then: 조회 결과 검증
        assertNotNull(projectPage, "프로젝트 페이지가 반환되어야 합니다");
        assertFalse(projectPage.isEmpty(), "프로젝트 목록이 비어있지 않아야 합니다");
        assertTrue(projectPage.getTotalElements() > 0, "전체 항목 수가 0보다 커야 합니다");
        assertEquals(0, projectPage.getNumber(), "현재 페이지 번호가 0이어야 합니다");
        assertTrue(projectPage.getSize() >= projectPage.getContent().size(),
                "페이지 크기가 올바르게 설정되어야 합니다");
    }

    @Test
    @DisplayName("프로젝트 ID로 프로젝트를 조회할 수 있어야 한다")
    void readProject_ShouldRetrieveProject() {
        // Given: 조회할 프로젝트 ID
        String projectId = EXISTING_PROJECT_ID;

        // When: 프로젝트 조회
        Optional<Project> foundProject = projectRepository.findByProjectId(projectId);

        // Then: 조회 결과 검증
        assertTrue(foundProject.isPresent(), "프로젝트가 조회되어야 합니다");
        Project project = foundProject.get();
        assertEquals("수정된 프로젝트명", project.getProjectName(), "프로젝트 이름이 일치해야 합니다");
        assertEquals("테스트 운영 고객사", project.getCustomerName(), "고객사 이름이 일치해야 합니다");
        assertEquals(TEST_STATUS_CODE, project.getStatusCode(), "상태 코드가 일치해야 합니다");
        assertFalse(project.getProjAssignedUsers().isEmpty(), "할당된 사용자가 있어야 합니다");
    }

    @Test
    @DisplayName("프로젝트 정보를 업데이트할 수 있어야 한다")
    @Transactional
    void updateProject_ShouldUpdateProject() {
        // Given: 업데이트할 프로젝트 및 데이터 준비
        Project foundProject = projectRepository.findByProjectId(EXISTING_PROJECT_ID)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + EXISTING_PROJECT_ID));

        String updatedName = "업데이트된 프로젝트";
        String updatedUrl = "http://updated.example.com";
        String updatedCustomer = "업데이트된 고객사";
        Set<String> updatedUserIds = new HashSet<>(List.of(TEST_ASSIGNEE, "DM1@test.co.kr"));

        // When: 프로젝트 정보 업데이트
        updateProjectInfo(foundProject, updatedName, updatedUrl, updatedCustomer, updatedUserIds);
        projectRepository.save(foundProject);

        // Then: 업데이트 결과 검증
        Project updatedProject = projectRepository.findByProjectId(EXISTING_PROJECT_ID)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다"));

        assertEquals(updatedName, updatedProject.getProjectName(), "프로젝트 이름이 업데이트되어야 합니다");
        assertEquals(updatedUrl, updatedProject.getUrlInfo(), "URL이 업데이트되어야 합니다");
        assertEquals(updatedCustomer, updatedProject.getCustomerName(), "고객사 이름이 업데이트되어야 합니다");
        assertEquals(updatedUserIds.size(), updatedProject.getProjAssignedUsers().size(),
                "할당된 사용자 수가 업데이트되어야 합니다");
    }

    @Test
    @DisplayName("프로젝트를 삭제할 수 있어야 한다")
    @Transactional
    void deleteProject_ShouldRemoveProject() {
        // Given: 삭제할 테스트 프로젝트 생성
        String projectIdToDelete = projectRepository.generateProjectIdUsingSequence();
        Project projectToDelete = createTestProject(projectIdToDelete, createTestUserSet());
        Project savedProject = projectRepository.save(projectToDelete);

        // 저장 확인
        assertTrue(projectRepository.findByProjectId(projectIdToDelete).isPresent(),
                "테스트용 프로젝트가 저장되어야 합니다");

        // When: 프로젝트 삭제
        projectRepository.delete(savedProject);

        // Then: 삭제 결과 확인
        Optional<Project> deletedProject = projectRepository.findByProjectId(projectIdToDelete);
        assertFalse(deletedProject.isPresent(), "프로젝트가 삭제되어야 합니다");
    }

    // 테스트 사용자 목록 생성
    private Set<String> createTestUserSet() {
        return new HashSet<>(List.of(
                TEST_ASSIGNEE,
                "qa2@test.co.kr",
                "DM1@test.co.kr",
                "DM2@test.co.kr"
        ));
    }

    // 테스트 프로젝트 생성
    private Project createTestProject(String projectId, Set<String> assignedUserIds) {
        return Project.builder()
                .projectId(projectId)
                .projectName("테스트 프로젝트")
                .urlInfo("http://test.example.com")
                .customerName("테스트 고객사")
                .statusCode(testStatusCode.getSeCode())
                .etcInfo("테스트용 프로젝트")
                .useYn("Y")
                .assignee(testUser)
                .createdBy(testUser.getUserId())
                .updatedBy(testUser.getUserId())
                .projAssignedUsers(assignedUserIds)
                .build();
    }

    // 프로젝트 정보 업데이트
    private void updateProjectInfo(Project project, String name, String url, String customer, Set<String> userIds) {
        // 프로젝트 정보 업데이트
        project.updateProjectInfo(
                name,
                url,
                customer,
                TEST_STATUS_CODE,
                "업데이트된 추가 정보",
                "Y",
                TEST_ASSIGNEE
        );

        // 할당 사용자 목록 업데이트
        project.getProjAssignedUsers().clear();
        for (String userId : userIds) {
            ProjectAssignUser assignUser = ProjectAssignUser.builder()
                    .projectId(project.getProjectId())
                    .userId(userId)
                    .build();
            project.addProjAssignedUser(assignUser);
        }
    }
}