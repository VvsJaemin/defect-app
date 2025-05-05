package com.group.defectapp.repository;

import com.group.defectapp.controller.file.util.FileUtil;
import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.defect.DefectListDto;
import com.group.defectapp.dto.defect.DefectSearchCondition;
import com.group.defectapp.repository.cmCode.CommonCodeRepository;
import com.group.defectapp.repository.defect.DefectRepository;
import com.group.defectapp.repository.defectlog.DefectLogRepository;
import com.group.defectapp.repository.project.ProjectRepository;
import com.group.defectapp.repository.user.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
        static final String TEST_ASSIGNEE = "jm0820@groovysoft.co.kr";
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

    @Autowired private DefectRepository defectRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DefectLogRepository defectLogRepository;
    @Autowired private CommonCodeRepository commonCodeRepository;
    @Autowired private FileUtil fileUtil;
    @Autowired private ProjectRepository projectRepository;

    /**
     * 지정된 코드를 조회하는 헬퍼 메소드
     */
    private CommonCode findCodeBySeCode(String seCode) {
        return commonCodeRepository.findBySeCode(seCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(TestConstants.ERROR_CODE_NOT_FOUND, seCode)));
    }

    /**
     * 테스트용 결함 객체를 생성하는 헬퍼 메소드
     */
    private Defect createTestDefect(String title, String content, User user, Project project) {
        CommonCode statusCode = findCodeBySeCode(TestConstants.DEFECT_STATUS);
        CommonCode orderCode = findCodeBySeCode(TestConstants.DEFECT_ORDER);
        CommonCode seriousCode = findCodeBySeCode(TestConstants.DEFECT_SERIOUS);
        CommonCode defectDiv = findCodeBySeCode(TestConstants.DEFECT_DIV);
        
        String newDefectId = defectRepository.generateDefectIdUsingSequence();
        
        return Defect.builder()
                .defectId(newDefectId)
                .projectId(project.getProjectId())
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
                .createdBy(user.getUserId())
                .build();
    }

    @Test
    @Order(1)
    @Transactional
    @Commit
    public void createMultipleDefectsWithFiles() {
        // Given
        User user = userRepository.findByUserId(TestConstants.TEST_USER_ID)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(TestConstants.ERROR_USER_NOT_FOUND, TestConstants.TEST_USER_ID)));
        
        Project project = projectRepository.findByProjectId(TestConstants.TEST_PROJECT_ID)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(TestConstants.ERROR_PROJECT_NOT_FOUND, TestConstants.TEST_PROJECT_ID)));

        // When
        for (int i = 0; i <= 5; i++) {
            Defect defect = createTestDefect(
                    "결함 제목" + i,
                    "결함 사항 " + i + "입니다.",
                    user,
                    project
            );

            // Then - 결함 파일 추가
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
            defectRepository.save(defect);
        }
    }

    @Test
    public void findDefectsBySearchCondition() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        DefectSearchCondition condition = DefectSearchCondition.builder()
                .searchType("defect_id")
                .searchText(TestConstants.TEST_SEARCH_DEFECT_ID)
                .build();
        
        // When
        Page<DefectListDto> defectList = defectRepository.list(pageable, condition);
        
        // Then
        System.out.println("검색된 결함 목록: " + defectList.getContent());
    }

    @Test
    public void findDefectById() {
        // Given
        String defectId = TestConstants.TEST_DEFECT_ID;
        
        // When
        Optional<Defect> defectOptional = defectRepository.findByDefectId(defectId);
        
        // Then
        if (defectOptional.isPresent()) {
            Defect defect = defectOptional.get();
            System.out.println("조회된 결함: " + defect);
        } else {
            System.out.println("결함을 찾을 수 없습니다: " + defectId);
        }
    }

    @Test
    @Transactional
    @Commit
    public void updateDefectTitle() {
        // Given
        String defectId = TestConstants.TEST_DEFECT_ID;
        String newTitle = "타이틀변경";
        
        // When
        Defect defect = defectRepository.findByDefectId(defectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(TestConstants.ERROR_DEFECT_NOT_FOUND, defectId)));
        
        // Then
        defect.changeDefectTitle(newTitle);
    }

    @Test
    @Transactional
    @Commit
    public void deleteDefect() {
        // Given
        String defectId = TestConstants.TEST_DEFECT_ID;
        
        // When
        Defect defect = defectRepository.findByDefectId(defectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(TestConstants.ERROR_DEFECT_NOT_FOUND, defectId)));
        
        // Then
        defectRepository.delete(defect);
    }
}