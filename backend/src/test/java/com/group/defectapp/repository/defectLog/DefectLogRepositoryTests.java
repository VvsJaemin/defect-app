package com.group.defectapp.repository.defectLog;

import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.defectlog.DefectLog;
import com.group.defectapp.domain.defectlog.DefectLogFile;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.repository.cmCode.CommonCodeRepository;
import com.group.defectapp.repository.defect.DefectRepository;
import com.group.defectapp.repository.defectlog.DefectLogRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DefectLogRepositoryTests {

    @Autowired
    private DefectLogRepository defectLogRepository;
    @Autowired
    private DefectRepository defectRepository;
    @Autowired
    private CommonCodeRepository commonCodeRepository;
    @Autowired
    private UserRepository userRepository;

    // 테스트 상수
    private static final String DEFECT_STATUS = "DS2000";
    private static final String TEST_USER_ID = "qa1@test.co.kr";
    private static final String TEST_DEFECT_ID = "DT0000000015";
    private static final String TEST_SEARCH_DEFECT_ID = "DT0000000004";
    private static final String UPLOAD_PATH = "uploads/defects";
    private static final String TEST_FILE_NAME = "_test.jpg";
    private static final String TEST_SYS_FILE_NAME = "sys_test.jpg";
    private static final String TEST_LOG_TITLE = "신규 결함이 등록되었습니다.";
    private static final String TEST_LOG_CONTENT = "결함이 여기에 있습니다.";

    private static final String ERROR_USER_NOT_FOUND = "해당 사용자를 찾을 수 없습니다: %s";
    private static final String ERROR_DEFECT_NOT_FOUND = "해당 결함을 찾을 수 없습니다: %s";
    private static final String ERROR_CODE_NOT_FOUND = "해당 코드를 찾을 수 없습니다: %s";

    private User testUser;
    private Defect testDefect;
    private CommonCode testStatusCode;

    @BeforeAll
    void setUpTestData() {
        // 테스트에 필요한 데이터 확인 및 초기화
        testUser = getUserOrThrow(TEST_USER_ID);
        testDefect = getDefectOrThrow(TEST_DEFECT_ID);
        testStatusCode = getCodeOrThrow(DEFECT_STATUS);

        assertNotNull(testUser, "테스트 사용자가 초기화되어야 합니다");
        assertNotNull(testDefect, "테스트 결함이 초기화되어야 합니다");
        assertNotNull(testStatusCode, "테스트 상태 코드가 초기화되어야 합니다");
    }

    @Test
    @Transactional
    @Order(1)
    @DisplayName("결함 로그를 저장하면 파일 정보와 함께 DB에 저장되어야 한다")
    public void saveDefectLog_ShouldPersistLogWithFile() {
        // given
        DefectLog defectLog = buildTestDefectLog();

        // when
        DefectLog savedLog = defectLogRepository.save(defectLog);

        // then
        assertAll(
                () -> assertNotNull(savedLog.getLogSeq(), "로그 시퀀스가 생성되어야 합니다"),
                () -> assertEquals(TEST_DEFECT_ID, savedLog.getDefectId(), "결함 ID가 일치해야 합니다"),
                () -> assertEquals(TEST_LOG_TITLE, savedLog.getLogTitle(), "로그 제목이 일치해야 합니다"),
                () -> assertEquals(TEST_LOG_CONTENT, savedLog.getLogCt(), "로그 내용이 일치해야 합니다"),
                () -> assertEquals(DEFECT_STATUS, savedLog.getStatusCd(), "상태 코드가 일치해야 합니다"),
                () -> assertEquals(TEST_USER_ID, savedLog.getCreatedBy(), "생성자가 일치해야 합니다"),
                () -> assertNotNull(savedLog.getCreatedAt(), "생성 시간이 설정되어야 합니다"),
                () -> assertFalse(savedLog.getDefectLogFiles().isEmpty(), "결함 로그 파일이 존재해야 합니다"),
                () -> assertEquals(1, savedLog.getDefectLogFiles().size(), "결함 로그 파일이 1개여야 합니다")
        );

        // 파일 정보 검증
        DefectLogFile logFile = savedLog.getDefectLogFiles().first();
        assertAll(
                () -> assertEquals(TEST_FILE_NAME, logFile.getOrg_file_name(), "원본 파일명이 일치해야 합니다"),
                () -> assertEquals(TEST_SYS_FILE_NAME, logFile.getSys_file_name(), "시스템 파일명이 일치해야 합니다"),
                () -> assertEquals(UPLOAD_PATH, logFile.getFile_path(), "파일 경로가 일치해야 합니다"),
                () -> assertEquals(TEST_DEFECT_ID, logFile.getDefectId(), "결함 ID가 일치해야 합니다")
        );
    }

    @Test
    @Order(2)
    @DisplayName("결함 ID로 결함 로그 목록을 조회하면 모든 관련 로그가 반환되어야 한다")
    public void findDefectLogsByDefectId_ShouldReturnAllRelatedLogs() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        // when
        var logs = defectLogRepository.list(pageable, TEST_DEFECT_ID);

        // then
        assertAll(
                () -> assertNotNull(logs, "결함 로그 목록이 반환되어야 합니다"),
                () -> assertFalse(logs.isEmpty(), "결함 로그 목록이 비어있지 않아야 합니다"),
                () -> assertTrue(logs.getTotalElements() > 0, "최소 1개 이상의 로그가 있어야 합니다")
        );

        // 모든 로그가 동일한 결함 ID를 가지고 있는지 확인
        logs.forEach(log -> {
            assertEquals(TEST_DEFECT_ID, log.getDefectId(),
                    "모든 로그는 요청한 결함 ID와 일치해야 합니다: " + log.getDefectId());
        });
    }

    @Test
    @Transactional
    @Order(3)
    @DisplayName("동일한 결함에 대해 여러 로그를 추가할 수 있어야 한다")
    public void addMultipleLogsToSameDefect_ShouldPersistAll() {
        // given
        int initialCount = defectLogRepository.findAllByDefectId(TEST_DEFECT_ID).size();

        // when
        DefectLog log1 = buildTestDefectLog("첫 번째 추가 로그", "내용 1");
        DefectLog log2 = buildTestDefectLog("두 번째 추가 로그", "내용 2");

        defectLogRepository.save(log1);
        defectLogRepository.save(log2);

        // then
        int finalCount = defectLogRepository.findAllByDefectId(TEST_DEFECT_ID).size();
        assertEquals(initialCount + 2, finalCount,
                "기존 로그 수에 2개의 로그가 추가되어야 합니다");
    }

    @Test
    @Transactional
    @Order(4)
    @DisplayName("결함 로그 파일을 제거할 수 있어야 한다")
    public void clearDefectLogFiles_ShouldRemoveAllFiles() {
        // given
        DefectLog defectLog = buildTestDefectLog();
        DefectLog savedLog = defectLogRepository.save(defectLog);
        assertFalse(savedLog.getDefectLogFiles().isEmpty(), "저장 직후에는 파일이 있어야 합니다");

        // when
        savedLog.clearDefectLogFiles();
        DefectLog updatedLog = defectLogRepository.save(savedLog);

        // then
        assertTrue(updatedLog.getDefectLogFiles().isEmpty(), "파일이 모두 제거되어야 합니다");
    }

    // 테스트용 결함 로그 생성
    private DefectLog buildTestDefectLog() {
        DefectLog defectLog = DefectLog.builder()
                .defectId(testDefect.getDefectId())
                .logTitle(TEST_LOG_TITLE)
                .logCt(TEST_LOG_CONTENT)
                .statusCd(testStatusCode.getSeCode())
                .createdBy(testUser.getUserId())
                .build();

        defectLog.addDefectLogFile(
                defectLog.getDefectId(),
                TEST_FILE_NAME,
                TEST_SYS_FILE_NAME,
                UPLOAD_PATH
        );

        return defectLog;
    }

    // 다른 제목과 내용으로 테스트용 결함 로그 생성
    private DefectLog buildTestDefectLog(String title, String content) {
        DefectLog defectLog = DefectLog.builder()
                .defectId(testDefect.getDefectId())
                .logTitle(title)
                .logCt(content)
                .statusCd(testStatusCode.getSeCode())
                .createdBy(testUser.getUserId())
                .build();

        return defectLog;
    }

    private User getUserOrThrow(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(String.format(ERROR_USER_NOT_FOUND, userId)));
    }

    private Defect getDefectOrThrow(String defectId) {
        return defectRepository.findByDefectId(defectId)
                .orElseThrow(() -> new IllegalArgumentException(String.format(ERROR_DEFECT_NOT_FOUND, defectId)));
    }

    private CommonCode getCodeOrThrow(String seCode) {
        return commonCodeRepository.findBySeCode(seCode)
                .orElseThrow(() -> new IllegalArgumentException(String.format(ERROR_CODE_NOT_FOUND, seCode)));
    }
}