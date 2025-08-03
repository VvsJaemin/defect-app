package com.group.defectapp.service.defectLog;

import com.group.defectapp.dto.common.PageRequestDto;
import com.group.defectapp.dto.defectlog.DefectLogListDto;
import com.group.defectapp.dto.defectlog.DefectLogRequestDto;
import com.group.defectapp.service.cmCode.CommonCodeService;
import com.group.defectapp.service.defectlog.DefectLogService;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DefectLogServiceTests {

    @Autowired
    private DefectLogService defectLogService;

    @Autowired
    private CommonCodeService commonCodeService;

    // 테스트 상수
    private static final String DEFECT_STATUS = "DS2000";
    private static final String TEST_USER_ID = "qa1@test.co.kr";
    private static final String TEST_DEFECT_ID = "DT0000000015";
    private static final String TEST_SEARCH_DEFECT_ID = "DT0000000004";
    private static final String UPLOAD_PATH = "uploads/defects";
    private static final String TEST_LOG_TITLE = "주문관리 결함 등록입니다.";
    private static final String TEST_LOG_CONTENT = "주문관리 아직 미 결함 처리";

    private static final String ERROR_USER_NOT_FOUND = "해당 사용자를 찾을 수 없습니다: %s";
    private static final String ERROR_DEFECT_NOT_FOUND = "해당 결함을 찾을 수 없습니다: %s";
    private static final String ERROR_CODE_NOT_FOUND = "해당 코드를 찾을 수 없습니다: %s";

    @Test
    @Order(1)
    @DisplayName("결함 로그를 저장할 수 있어야 한다")
    @Transactional
    public void saveDefectLog_ShouldPersistDefectLog() {
        // Given: 결함 로그 요청 DTO 및 파일 준비
        DefectLogRequestDto defectLogRequestDto = createTestDefectLogRequest();
        MultipartFile[] files = createTestMultipartFiles();

        // When: 결함 로그 저장
        Long logId = defectLogService.defectLogSave(defectLogRequestDto, files);

        // Then: 결함 로그가 성공적으로 저장되었는지 확인
        assertNotNull(logId, "저장된 결함 로그 ID가 null이 아니어야 합니다");
        assertTrue(logId > 0, "저장된 결함 로그 ID가 유효해야 합니다");
    }

    @Test
    @Order(2)
    @DisplayName("결함 ID로 결함 로그 목록을 조회할 수 있어야 한다")
    public void defectLogList_ShouldReturnDefectLogList() {
        // Given: 결함 ID 및 페이지 요청 준비
        PageRequestDto pageRequestDto = PageRequestDto.builder()
                .pageIndex(0)
                .pageSize(10)
                .build();

        // When: 결함 로그 목록 조회
        Page<DefectLogListDto> defectLogList = defectLogService.defectLogList(pageRequestDto, TEST_DEFECT_ID);

        // Then: 결함 로그 목록 검증
        assertNotNull(defectLogList, "결함 로그 목록이 null이 아니어야 합니다");
        assertNotNull(defectLogList.getContent(), "결함 로그 목록 내용이 null이 아니어야 합니다");

        // 페이징 정보 검증
        assertEquals(0, defectLogList.getNumber(), "현재 페이지는 0이어야 합니다");
        assertTrue(defectLogList.getSize() >= defectLogList.getContent().size(),
                "페이지 크기가 올바르게 설정되어야 합니다");


        // 결함 로그 항목에 대한 추가 검증
        if (!defectLogList.isEmpty()) {
            DefectLogListDto firstLog = defectLogList.getContent().get(0);
            assertEquals(TEST_DEFECT_ID, firstLog.getDefectId(),
                    "조회된 로그의 결함 ID가 요청한 ID와 일치해야 합니다");
        }
    }

    // 테스트용 결함 로그 요청 DTO 생성
    private DefectLogRequestDto createTestDefectLogRequest() {
        return DefectLogRequestDto.builder()
                .defectId(TEST_DEFECT_ID)
                .logTitle(TEST_LOG_TITLE)
                .logCt(TEST_LOG_CONTENT)
                .statusCd(commonCodeService.findBySeCode(DEFECT_STATUS).getSeCode())
                .createdBy(TEST_USER_ID)
                .build();
    }

    // 테스트용 파일 생성
    private MultipartFile[] createTestMultipartFiles() {
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "test_file.txt",
                "text/plain",
                "테스트 파일 내용입니다.".getBytes(StandardCharsets.UTF_8)
        );
        return new MultipartFile[]{file1};
    }
}