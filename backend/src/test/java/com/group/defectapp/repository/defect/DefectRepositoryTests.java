package com.group.defectapp.repository.defect;

import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.defect.DefectListDto;
import com.group.defectapp.dto.defect.DefectSearchCondition;
import com.group.defectapp.repository.cmCode.CommonCodeRepository;
import com.group.defectapp.repository.project.ProjectRepository;
import com.group.defectapp.repository.user.UserRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DefectRepositoryTests {
    // 상수 정의
    private static class TestConstants {
        // 코드 상수
        static final String DEFECT_DIV = "FUNCTION";
        static final String DEFECT_SERIOUS = "5";
        static final String DEFECT_ORDER = "MOMETLY";
        static final String DEFECT_STATUS = "DS2000";

        // 테스트 데이터 상수
        static final String TEST_PROJECT_ID = "PROJ000010";
        static final String TEST_ASSIGNEE = "jm0820@test.co.kr";
        static final String TEST_USER_ID = "qa1@test.co.kr";
        static final String TEST_DEFECT_ID = "DT0000000006";
        static final String TEST_SEARCH_DEFECT_ID = "DT0000000004";

        // 파일 경로 상수
        static final String UPLOAD_PATH = "uploads/defects";

        // 오류 메시지 상수
        static final String ERROR_USER_NOT_FOUND = "해당 사용자를 찾을 수 없습니다: %s";
        static final String ERROR_PROJECT_NOT_FOUND = "해당 프로젝트를 찾을 수 없습니다: %s";
        static final String ERROR_CODE_NOT_FOUND = "해당 코드를 찾을 수 없습니다: %s";
        static final String ERROR_DEFECT_NOT_FOUND = "해당 결함을 찾을 수 없습니다: %s";
    }

    @Autowired
    private DefectRepository defectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommonCodeRepository commonCodeRepository;
    @Autowired
    private ProjectRepository projectRepository;

    private User testUser;
    private Project testProject;

    @BeforeAll
    void setUpTestData() {
        // 테스트에 필요한 기본 데이터 설정
        testUser = userRepository.findByUserId(TestConstants.TEST_USER_ID)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(TestConstants.ERROR_USER_NOT_FOUND, TestConstants.TEST_USER_ID)));

        testProject = projectRepository.findByProjectId(TestConstants.TEST_PROJECT_ID)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(TestConstants.ERROR_PROJECT_NOT_FOUND, TestConstants.TEST_PROJECT_ID)));
    }

    /**
     * 지정된 코드를 조회
     */
    private CommonCode findCodeBySeCode(String seCode) {
        return commonCodeRepository.findBySeCode(seCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(TestConstants.ERROR_CODE_NOT_FOUND, seCode)));
    }

    /**
     * 테스트용 결함 객체를 생성
     */
    private Defect createTestDefect(String title, String content) {
        CommonCode statusCode = findCodeBySeCode(TestConstants.DEFECT_STATUS);
        CommonCode orderCode = findCodeBySeCode(TestConstants.DEFECT_ORDER);
        CommonCode seriousCode = findCodeBySeCode(TestConstants.DEFECT_SERIOUS);
        CommonCode defectDiv = findCodeBySeCode(TestConstants.DEFECT_DIV);

        String newDefectId = defectRepository.generateDefectIdUsingSequence();

        return Defect.builder()
                .defectId(newDefectId)
                .projectId(testProject.getProjectId())
                .defectTitle(title)
                .defectContent(content)
                .assignee(TestConstants.TEST_ASSIGNEE)
                .statusCode(statusCode.getSeCode())
                .seriousCode(seriousCode.getSeCode())
                .orderCode(orderCode.getSeCode())
                .defectDivCode(defectDiv.getSeCode())
                .defectMenuTitle("주문 > 주문관리")
                .defectUrlInfo("http://test.example.com")
                .openYn("Y")
                .createdBy(testUser.getUserId())
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("여러 결함을 파일과 함께 생성할 수 있어야 한다")
    @Transactional
    @Commit
    public void createMultipleDefectsWithFiles() {
        // Given
        int defectCount = 5;

        // When: 여러 결함 생성 및 저장
        for (int i = 0; i <= defectCount; i++) {
            Defect defect = createTestDefect(
                    "결함 제목" + i,
                    "결함 사항 " + i + "입니다."
            );

            // 결함 파일 추가
            defect.addDefectFile(
                    "2tes2t_excel.xlsx",
                    "sys_test_excel.xlsx",
                    TestConstants.UPLOAD_PATH
            );
            defect.addDefectFile(
                    i + "_test2.jpg",
                    "sys_" + i + "_test2.jpg",
                    TestConstants.UPLOAD_PATH
            );
            Defect savedDefect = defectRepository.save(defect);

            // Then: 저장 확인
            assertNotNull(savedDefect.getDefectId(), "결함 ID가 생성되어야 합니다");
            assertEquals(2, savedDefect.getDefectFiles().size(), "결함 파일이 2개 추가되어야 합니다");
        }
    }

    @Test
    @DisplayName("검색 조건으로 결함을 조회할 수 있어야 한다")
    public void findDefectsBySearchCondition() {
        // Given: 페이징 및 검색 조건 설정
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        DefectSearchCondition condition = DefectSearchCondition.builder()
                .searchType("defect_id")
                .searchText(TestConstants.TEST_SEARCH_DEFECT_ID)
                .build();

        // When: 결함 목록 조회
        Page<DefectListDto> defectList = defectRepository.list(pageable, condition);

        // Then: 조회 결과 검증
        assertNotNull(defectList, "결함 목록이 반환되어야 합니다");
        assertFalse(defectList.isEmpty(), "검색 결과가 비어있지 않아야 합니다");

        defectList.getContent().forEach(defect ->
                assertTrue(defect.getDefectId().contains(TestConstants.TEST_SEARCH_DEFECT_ID),
                        "검색된 결함 ID는 검색어를 포함해야 합니다"));
    }

    @Test
    @DisplayName("결함 ID로 결함을 조회할 수 있어야 한다")
    public void findDefectById() {
        // Given: 조회할 결함 ID
        String defectId = TestConstants.TEST_DEFECT_ID;

        // When: 결함 조회
        Optional<Defect> defectOptional = defectRepository.findByDefectId(defectId);

        // Then: 조회 결과 검증
        assertTrue(defectOptional.isPresent(), "결함이 조회되어야 합니다");

        Defect defect = defectOptional.get();
        assertEquals(defectId, defect.getDefectId(), "조회된 결함 ID가 일치해야 합니다");
        assertNotNull(defect.getDefectTitle(), "결함 제목이 있어야 합니다");
    }

    @Test
    @DisplayName("결함 제목을 수정할 수 있어야 한다")
    @Transactional
    @Commit
    public void updateDefectTitle() {
        // Given: 수정할 결함 및 새 제목
        String defectId = TestConstants.TEST_DEFECT_ID;
        String newTitle = "타이틀변경";

        // When: 결함 조회 및 제목 변경
        Defect defect = defectRepository.findByDefectId(defectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(TestConstants.ERROR_DEFECT_NOT_FOUND, defectId)));

        defect.changeDefectTitle(newTitle);
        Defect updatedDefect = defectRepository.save(defect);

        // Then: 변경 결과 검증
        assertEquals(newTitle, updatedDefect.getDefectTitle(), "결함 제목이 변경되어야 합니다");

        // DB에서 다시 조회하여 영구적으로 변경되었는지 확인
        Defect retrievedDefect = defectRepository.findByDefectId(defectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(TestConstants.ERROR_DEFECT_NOT_FOUND, defectId)));
        assertEquals(newTitle, retrievedDefect.getDefectTitle(), "DB에 저장된 결함 제목이 변경되어야 합니다");
    }

    @Test
    @DisplayName("결함을 삭제할 수 있어야 한다")
    @Transactional
    public void deleteDefect() {
        // Given: 삭제할 테스트 결함 생성
        Defect testDefect = createTestDefect("삭제 테스트 결함", "삭제될 결함입니다");
        Defect savedDefect = defectRepository.save(testDefect);
        String defectIdToDelete = savedDefect.getDefectId();

        // 저장 확인
        assertTrue(defectRepository.findByDefectId(defectIdToDelete).isPresent(),
                "테스트용 결함이 저장되어야 합니다");

        // When: 결함 삭제
        defectRepository.delete(savedDefect);

        // Then: 삭제 결과 확인
        Optional<Defect> deletedDefect = defectRepository.findByDefectId(defectIdToDelete);
        assertFalse(deletedDefect.isPresent(), "결함이 삭제되어야 합니다");
    }
}