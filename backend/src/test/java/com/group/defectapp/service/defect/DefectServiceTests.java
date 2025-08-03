package com.group.defectapp.service.defect;

import com.group.defectapp.controller.file.util.FileUtil;
import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.dto.defect.DefectListDto;
import com.group.defectapp.dto.defect.DefectRequestDto;
import com.group.defectapp.dto.defect.DefectResponseDto;
import com.group.defectapp.dto.defect.DefectSearchCondition;
import com.group.defectapp.dto.common.PageRequestDto;
import com.group.defectapp.repository.defect.DefectRepository;
import com.group.defectapp.service.cmCode.CommonCodeService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DefectServiceTests {

    @Autowired
    private DefectService defectService;

    @Autowired
    private DefectRepository defectRepository;

    @Autowired
    private CommonCodeService commonCodeService;

    @Autowired
    private FileUtil fileUtil;

    private String createdDefectId;

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
        static final String TEST_DEFECT_ID = "DT000000000722";
        static final String TEST_SEARCH_DEFECT_ID = "DT0000000004";

        // 파일 경로 상수
        static final String UPLOAD_PATH = "uploads/defects";

        // 오류 메시지 상수
        static final String ERROR_USER_NOT_FOUND = "해당 사용자를 찾을 수 없습니다: %s";
        static final String ERROR_PROJECT_NOT_FOUND = "해당 프로젝트를 찾을 수 없습니다: %s";
        static final String ERROR_CODE_NOT_FOUND = "해당 코드를 찾을 수 없습니다: %s";
        static final String ERROR_DEFECT_NOT_FOUND = "해당 결함을 찾을 수 없습니다: %s";
    }

    @BeforeAll
    void setUp() {
        // 테스트에 필요한 코드가 존재하는지 확인
        assertNotNull(commonCodeService.findBySeCode(TestConstants.DEFECT_STATUS),
                "상태 코드가 DB에 존재해야 합니다");
        assertNotNull(commonCodeService.findBySeCode(TestConstants.DEFECT_ORDER),
                "우선순위 코드가 DB에 존재해야 합니다");
        assertNotNull(commonCodeService.findBySeCode(TestConstants.DEFECT_SERIOUS),
                "심각도 코드가 DB에 존재해야 합니다");
        assertNotNull(commonCodeService.findBySeCode(TestConstants.DEFECT_DIV),
                "결함 구분 코드가 DB에 존재해야 합니다");
    }

    @Test
    @Order(1)
    @DisplayName("결함을 등록하면 DB에 저장되어야 한다")
    @Transactional
    public void saveDefect_ShouldPersistDefect() {
        // Given: 결함 등록에 필요한 데이터 준비
        String loginUserId = TestConstants.TEST_USER_ID;

        CommonCode statusCode = commonCodeService.findBySeCode(TestConstants.DEFECT_STATUS);
        CommonCode orderCode = commonCodeService.findBySeCode(TestConstants.DEFECT_ORDER);
        CommonCode seriousCode = commonCodeService.findBySeCode(TestConstants.DEFECT_SERIOUS);
        CommonCode defectDiv = commonCodeService.findBySeCode(TestConstants.DEFECT_DIV);

        String testDefectTitle = "결함발생 테스트";
        String testDefectContent = "결함 처리해주세요 - 테스트";

        // 결함 등록 DTO 생성
        DefectRequestDto defectRequestDto = DefectRequestDto.builder()
                .projectId(TestConstants.TEST_PROJECT_ID)
                .defectTitle(testDefectTitle)
                .defectContent(testDefectContent)
                .statusCode(statusCode.getSeCode())
                .orderCode(orderCode.getSeCode())
                .seriousCode(seriousCode.getSeCode())
                .defectDivCode(defectDiv.getSeCode())
                .assigneeId(TestConstants.TEST_ASSIGNEE)
                .defectMenuTitle("출고 > 출고관리 테스트")
                .defectUrlInfo("https://test.example.com")
                .openYn("Y")
                .build();

        // 테스트용 파일 생성
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "file1.txt", "text/plain", "테스트 파일".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "file2.txt", "text/plain", "두번째 파일".getBytes(StandardCharsets.UTF_8));
        MultipartFile[] files = new MultipartFile[]{file1, file2};

        // When: 결함 등록 서비스 호출
        defectService.saveDefect(defectRequestDto, files, loginUserId);

        // Then: 저장된 결함 확인 (ID로 검색하여 제목과 내용 확인)
        // 가장 최근에 등록된 결함 조회
        PageRequestDto pageRequestDto = PageRequestDto.builder()
                .pageIndex(0)
                .pageSize(1)
                .build();

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("searchType", "defect_title");
        paramMap.put("searchText", testDefectTitle);

        Page<DefectListDto> result = defectService.defectList(pageRequestDto, paramMap);

        // 테스트 결과 검증
        assertFalse(result.isEmpty(), "결함이 저장되어야 합니다");
        DefectListDto savedDefect = result.getContent().get(0);
        assertEquals(testDefectTitle, savedDefect.getDefectTitle(), "결함 제목이 일치해야 합니다");

        // 이후 테스트에 사용할 ID 저장
        createdDefectId = savedDefect.getDefectId();
        assertNotNull(createdDefectId, "생성된 결함 ID가 존재해야 합니다");
    }

    @Test
    @Order(2)
    @DisplayName("검색 조건에 맞는 결함이 페이징되어 조회되어야 한다")
    public void getDefectList_ShouldReturnPagedResults() {
        // Given: 페이징 요청 정보 및 검색 조건 설정
        PageRequestDto pageRequestDto = PageRequestDto.builder()
                .pageIndex(0)
                .pageSize(10)
                .build();

        DefectSearchCondition condition = DefectSearchCondition.builder()
                .searchType("defect_id")
                .searchText(TestConstants.TEST_SEARCH_DEFECT_ID)
                .build();

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("searchType", condition.getSearchType());
        paramMap.put("searchText", condition.getSearchText());

        // When: 결함 목록 조회
        Page<DefectListDto> defectList = defectService.defectList(pageRequestDto, paramMap);

        // Then: 결과 검증
        assertNotNull(defectList, "결함 목록이 반환되어야 합니다");
        assertTrue(defectList.getTotalElements() > 0, "검색 결과가 존재해야 합니다");
        assertEquals(0, defectList.getNumber(), "페이지 번호가 일치해야 합니다");

        // 검색된 결과들이 검색어를 포함하는지 확인
        defectList.getContent().forEach(defect ->
            assertTrue(defect.getDefectId().contains(TestConstants.TEST_SEARCH_DEFECT_ID),
                    "검색된 결과는 검색어를 포함해야 합니다"));
    }

    @Test
    @Order(3)
    @DisplayName("결함 ID로 결함 상세 정보를 조회할 수 있어야 한다")
    public void getDefectById_ShouldReturnDefectDetails() {
        // Given: 조회할 결함 ID
        String defectId = TestConstants.TEST_DEFECT_ID;

        // When: 결함 상세 조회
        DefectResponseDto defectResponseDto = defectService.readDefect(defectId);

        // Then: 조회 결과 검증
        assertNotNull(defectResponseDto, "결함 상세 정보가 반환되어야 합니다");
        assertEquals(defectId, defectResponseDto.getDefectId(), "결함 ID가 일치해야 합니다");
        assertNotNull(defectResponseDto.getDefectTitle(), "결함 제목이 존재해야 합니다");
        assertNotNull(defectResponseDto.getDefectContent(), "결함 내용이 존재해야 합니다");
        assertNotNull(defectResponseDto.getStatusCode(), "상태 코드가 존재해야 합니다");
    }

    @Test
    @Order(4)
    @DisplayName("결함 정보를 수정할 수 있어야 한다")
    @Transactional
    public void modifyDefect_ShouldUpdateDefectInfo() {
        // Given: 수정할 결함 ID와 사용자 정보
        String defectId = createdDefectId != null ? createdDefectId : TestConstants.TEST_DEFECT_ID;
        Principal loginUser = () -> TestConstants.TEST_USER_ID;

        CommonCode statusCode = commonCodeService.findBySeCode(TestConstants.DEFECT_STATUS);
        CommonCode orderCode = commonCodeService.findBySeCode(TestConstants.DEFECT_ORDER);
        CommonCode seriousCode = commonCodeService.findBySeCode(TestConstants.DEFECT_SERIOUS);
        CommonCode defectDiv = commonCodeService.findBySeCode(TestConstants.DEFECT_DIV);

        String updatedTitle = "수정된 결함 제목";
        String updatedContent = "수정된 결함 내용입니다.";

        DefectRequestDto defectRequestDto = DefectRequestDto.builder()
                .projectId(TestConstants.TEST_PROJECT_ID)
                .defectId(defectId)
                .defectTitle(updatedTitle)
                .defectContent(updatedContent)
                .statusCode(statusCode.getSeCode())
                .orderCode(orderCode.getSeCode())
                .seriousCode(seriousCode.getSeCode())
                .defectDivCode(defectDiv.getSeCode())
                .assigneeId(TestConstants.TEST_ASSIGNEE)
                .defectMenuTitle("출고 > 출고관리")
                .defectUrlInfo("http://test.example.com/updated")
                .openYn("Y")
                .build();

        // 테스트용 파일 생성
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "updatefile1.txt", "text/plain", "수정 파일 내용".getBytes(StandardCharsets.UTF_8)
        );
        MultipartFile[] files = new MultipartFile[]{file1};

        // When: 결함 수정
        defectService.modifyDefect(defectRequestDto, files, loginUser);

        // Then: 수정된 결함 정보 확인
        DefectResponseDto updatedDefect = defectService.readDefect(defectId);

        assertNotNull(updatedDefect, "수정된 결함이 조회되어야 합니다");
        assertEquals(updatedTitle, updatedDefect.getDefectTitle(), "결함 제목이 수정되어야 합니다");
        assertEquals(updatedContent, updatedDefect.getDefectContent(), "결함 내용이 수정되어야 합니다");
    }

    @Test
    @Order(5)
    @DisplayName("존재하지 않는 결함 ID로 조회 시 예외가 발생해야 한다")
    public void getDefectByInvalidId_ShouldThrowException() {
        // Given: 존재하지 않는 결함 ID
        String invalidDefectId = "DT999999999999";

        // When & Then: 예외 발생 검증
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            defectService.readDefect(invalidDefectId);
        });

        assertTrue(exception.getMessage().contains("결함을 찾을 수 없습니다"),
                "적절한 예외 메시지가 포함되어야 합니다");
    }

    @Test
    @Order(6)
    @DisplayName("결함을 삭제할 수 있어야 한다")
    @Transactional
    public void deleteDefect_ShouldMarkDefectAsDeleted() {
        // Given: 삭제할 결함 ID와 사용자 정보
        String defectId = createdDefectId != null ? createdDefectId : TestConstants.TEST_DEFECT_ID;
        Principal loginUser = () -> TestConstants.TEST_USER_ID;

        // 삭제 전 결함 존재 확인
        DefectResponseDto beforeDelete = defectService.readDefect(defectId);
        assertNotNull(beforeDelete, "삭제할 결함이 존재해야 합니다");

        // When: 결함 삭제
        defectService.deleteDefect(defectId, loginUser);

        // Then: 결함이 삭제 처리되었는지 확인 (DB에서 완전히 삭제되지 않고 사용 여부가 'N'으로 변경)
        Defect deletedDefect = defectRepository.findById(defectId)
                .orElse(null);

        assertNotNull(deletedDefect, "결함은 여전히 DB에 존재해야 합니다");
        assertEquals("N", deletedDefect.getOpenYn(), "결함의 사용 여부가 'N'으로 변경되어야 합니다");
    }

    // 권한이 없는 사용자의 결함 삭제 시도에 대한 테스트
    @Test
    @Order(7)
    @DisplayName("권한이 없는 사용자가 결함을 삭제하려 할 경우 예외가 발생해야 한다")
    @Transactional
    public void deleteDefect_WithoutPermission_ShouldThrowException() {
        // Given: 다른 사용자가 등록한 결함 ID와 권한이 없는 사용자 정보
        String defectId = TestConstants.TEST_DEFECT_ID;
        Principal unauthorizedUser = () -> "unauthorized@test.co.kr";

        // When & Then: 예외 발생 검증
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            defectService.deleteDefect(defectId, unauthorizedUser);
        });

        // 권한 부족 관련 메시지가 포함되어 있는지 확인
        assertTrue(exception.getMessage().contains("권한이 없습니다") ||
                   exception.getMessage().contains("해당 사용자를 찾을 수 없습니다"),
                   "적절한 예외 메시지가 포함되어야 합니다");
    }
}