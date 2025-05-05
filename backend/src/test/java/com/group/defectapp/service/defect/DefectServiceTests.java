package com.group.defectapp.service.defect;

import com.group.defectapp.controller.file.util.FileUtil;
import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.dto.defect.*;
import com.group.defectapp.service.cmCode.CommonCodeService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DefectServiceTests {

    @Autowired
    private DefectService defectService;

    @Autowired
    private CommonCodeService commonCodeService;

    @Autowired
    private FileUtil fileUtil;

    // 상수 정의
    private static class TestConstants {
        // 코드 상수
        static final String DEFECT_DIV = "FUNCTION";
        static final String DEFECT_SERIOUS = "5";
        static final String DEFECT_ORDER = "MOMETLY";
        static final String DEFECT_STATUS = "DS2000";

        // 테스트 데이터 상수
        static final String TEST_PROJECT_ID = "PROJ000010";
        static final String TEST_ASSIGNEE = "jm0820@groovysoft.co.kr";
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

    @Test
    @Order(1)
    @Transactional
    @Commit
    public void saveDefect_ShouldPersistDefect() {

        String loginUserId = TestConstants.TEST_USER_ID;

        CommonCode statusCode = commonCodeService.findBySeCode(TestConstants.DEFECT_STATUS);
        CommonCode orderCode = commonCodeService.findBySeCode(TestConstants.DEFECT_ORDER);
        CommonCode seriousCode = commonCodeService.findBySeCode(TestConstants.DEFECT_SERIOUS);
        CommonCode defectDiv = commonCodeService.findBySeCode(TestConstants.DEFECT_DIV);

        // 결함 등록 DTO 생성
        DefectRequestDto defectRequestDto = DefectRequestDto.builder()
                .projectId(TestConstants.TEST_PROJECT_ID)
                .defectTitle("결함발생")
                .defectContent("결함 처리해주세요")
                .statusCode(statusCode.getSeCode())
                .orderCode(orderCode.getSeCode())
                .seriousCode(seriousCode.getSeCode())
                .defectDivCode(defectDiv.getSeCode())
                .assigneeId(TestConstants.TEST_ASSIGNEE)
                .defectMenuTitle("출고 > 출고관리2")
                .defectUrlInfo("https://test.example.com")
                .openYn("Y")
                .build();

        MockMultipartFile file1 = new MockMultipartFile(
                "files", "file1.txt", "text/plain", "테스트 파일".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "file2.txt", "text/plain", "두번째 파일".getBytes());
        MultipartFile[] files = new MultipartFile[]{file1, file2};

        defectService.saveDefect(defectRequestDto, files, loginUserId);

    }

    @Test
    @Order(2)
    public void getDefectList() {
        PageRequestDto pageRequestDto = PageRequestDto.builder()
                .page(0)
                .pageSize(10)
                .build();

        DefectSearchCondition condition = DefectSearchCondition.builder()
                .searchType("defect_id")
                .searchText("DT0000000015")
                .build();

        Map<String, Object> paramMap = new HashMap<>();

        paramMap.put("searchType", condition.getSearchType());
        paramMap.put("searchText", condition.getSearchText());

        Page<DefectListDto> defectList = defectService.defectList(pageRequestDto, paramMap);

        System.out.println("defectList.getContent() = " + defectList.getContent());

    }


    @Test
    @Order(3)
    public void getDefectById() {
        String defectId = TestConstants.TEST_DEFECT_ID;

        DefectResponseDto defectResponseDto = defectService.readDefect(defectId);

        System.out.println("defectResponseDto = " + defectResponseDto);
    }


    @Test
    public void updateDefectTitle() {
        String defectId = TestConstants.TEST_DEFECT_ID;
        Principal loginUserId = () -> TestConstants.TEST_USER_ID;


        CommonCode statusCode = commonCodeService.findBySeCode(TestConstants.DEFECT_STATUS);
        CommonCode orderCode = commonCodeService.findBySeCode(TestConstants.DEFECT_ORDER);
        CommonCode seriousCode = commonCodeService.findBySeCode(TestConstants.DEFECT_SERIOUS);
        CommonCode defectDiv = commonCodeService.findBySeCode(TestConstants.DEFECT_DIV);

        DefectRequestDto defectRequestDto = DefectRequestDto.builder()
                .projectId(TestConstants.TEST_PROJECT_ID)
                .defectId(defectId)
                .defectTitle("테스트 222결함 수정")
                .defectContent("수정된 결함 내용입니다.")
                .statusCode(statusCode.getSeCode())
                .orderCode(orderCode.getSeCode())
                .seriousCode(seriousCode.getSeCode())
                .defectDivCode(defectDiv.getSeCode())
                .assigneeId(TestConstants.TEST_ASSIGNEE)
                .defectMenuTitle("출고 > 출고관리")
                .defectUrlInfo("http://test.22example.com")
                .openYn("Y")
                .build();

        // 테스트용 파일 생성
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "updatefile1.txt", "text/plain", "수정 파일 내용".getBytes()
        );
        MultipartFile[] files = new MultipartFile[]{file1};

        // principal 대신 loginUserId 사용
        defectService.modifyDefect(defectRequestDto, files, loginUserId);
    }

    @Test
    public void deleteDefect() {
        String defectId = TestConstants.TEST_DEFECT_ID;
        Principal loginUserId = () -> TestConstants.TEST_USER_ID;

        defectService.deleteDefect(defectId, loginUserId);
    }


}
