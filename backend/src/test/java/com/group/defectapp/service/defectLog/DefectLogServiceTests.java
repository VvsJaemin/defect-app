package com.group.defectapp.service.defectLog;

import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.defectlog.DefectLogListDto;
import com.group.defectapp.dto.defectlog.DefectLogRequestDto;
import com.group.defectapp.service.cmCode.CommonCodeService;
import com.group.defectapp.service.defectlog.DefectLogService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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

    private static final String ERROR_USER_NOT_FOUND = "해당 사용자를 찾을 수 없습니다: %s";
    private static final String ERROR_DEFECT_NOT_FOUND = "해당 결함을 찾을 수 없습니다: %s";
    private static final String ERROR_CODE_NOT_FOUND = "해당 코드를 찾을 수 없습니다: %s";

    @Test
    @Transactional
    @Commit
    public void saveDefectLog() {
        DefectLogRequestDto defectLogRequestDto = DefectLogRequestDto.builder()
                .defectId(TEST_DEFECT_ID)
                .logTitle("주문관리 결함 등록입니다.")
                .logCt("주문관리 아직 미 결함 처리")
                .statusCd(commonCodeService.findBySeCode(DEFECT_STATUS).getSeCode())
                .createdBy(TEST_USER_ID)
                .build();

        MockMultipartFile file1 = new MockMultipartFile(
                "files", "file1.txt", "text/plain", "테스트 파일".getBytes());

        MultipartFile[] files = new MultipartFile[]{file1};

        defectLogService.defectLogSave(defectLogRequestDto, files);

    }

    @Test
    public void defectLogList() {
        String defectId = TEST_DEFECT_ID;
        PageRequestDto pageRequestDto = PageRequestDto.builder()
                .pageIndex(0)
                .pageSize(10)
                .build();

        Page<DefectLogListDto> defectLogList = defectLogService.defectLogList(pageRequestDto, defectId);

        System.out.println("defectLogList = " + defectLogList.getContent());

    }
}
