package com.group.defectapp.repository;

import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.defectlog.DefectLog;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.repository.cmCode.CommonCodeRepository;
import com.group.defectapp.repository.defect.DefectRepository;
import com.group.defectapp.repository.defectlog.DefectLogRepository;
import com.group.defectapp.repository.user.UserRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DefectLogRepositoryTests {

    @Autowired private DefectLogRepository defectLogRepository;
    @Autowired private DefectRepository defectRepository;
    @Autowired private CommonCodeRepository commonCodeRepository;
    @Autowired private UserRepository userRepository;

    // 테스트 상수
    private static final String DEFECT_STATUS = "DS2000";
    private static final String TEST_USER_ID = "qa1@test.co.kr";
    private static final String TEST_DEFECT_ID = "DT0000000015";
    private static final String TEST_SEARCH_DEFECT_ID = "DT0000000004";
    private static final String UPLOAD_PATH = "uploads/defects";

    private static final String ERROR_USER_NOT_FOUND = "해당 사용자를 찾을 수 없습니다: %s";
    private static final String ERROR_DEFECT_NOT_FOUND = "해당 결함을 찾을 수 없습니다: %s";
    private static final String ERROR_CODE_NOT_FOUND = "해당 코드를 찾을 수 없습니다: %s";

    @Test
    @Transactional
    @Commit
    public void saveDefectLog() {
        CommonCode statusCode = getCodeOrThrow(DEFECT_STATUS);
        User user = getUserOrThrow(TEST_USER_ID);
        Defect defect = getDefectOrThrow(TEST_DEFECT_ID);

        DefectLog defectLog = DefectLog.builder()
                .defectId(defect.getDefectId())
                .logTitle("신규 결함이 등록되었습니다.")
                .logCt("결함이 여기에 있습니다.")
                .statusCd(statusCode.getSeCode())
                .createdBy(user.getUserId())
                .build();

        defectLog.addDefectLogFile(
                defectLog.getDefectId(),
                "_test.jpg",
                "sys_teswt.jpg",
                UPLOAD_PATH
        );

        defectLogRepository.save(defectLog);
    }

    @Test
    public void defectLogList() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        defectLogRepository.list(pageable, TEST_DEFECT_ID)
                .forEach(System.out::println);
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