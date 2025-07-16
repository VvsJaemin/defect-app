
package com.group.defectapp.service.File;

import com.group.defectapp.controller.file.util.FileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileServiceTests {

    // 테스트 상수 정의 - 클래스 상수로 직접 노출하여 가독성 향상
    private static final String TEST_FILE_NAME = "test.txt";
    private static final String TEST_FILE_CONTENT = "테스트 파일 내용입니다.";
    private static final String TEST_CONTENT_TYPE = "text/plain";

    @Autowired
    private FileUtil fileUtil;

    @Value("${defect.upload.path}")
    private String uploadPath;

    // 테스트 간 공유되는 데이터
    private String savedFileName;
    private List<String> uploadedFiles = new ArrayList<>();

    // 테스트 파일 생성 메서드 - 코드 중복 제거
    private MockMultipartFile createTestFile() {
        return new MockMultipartFile(
                "file",
                TEST_FILE_NAME,
                TEST_CONTENT_TYPE,
                TEST_FILE_CONTENT.getBytes(StandardCharsets.UTF_8)
        );
    }

    @AfterEach
    void cleanUpAfterTest() {
        // 생성된 모든 파일을 정리
        for (String fileName : uploadedFiles) {
            if (fileUtil.fileExists(fileName)) {
                fileUtil.deleteFile(fileName);
            }
        }
        uploadedFiles.clear();
    }

    @Test
    @Order(1)
    @DisplayName("파일이 성공적으로 업로드되어야 한다")
    void uploadFile_ShouldUploadFileSuccessfully() throws Exception {
        // given: 테스트 파일 준비
        MockMultipartFile mockMultipartFile = createTestFile();

        // when: 파일 업로드
        savedFileName = fileUtil.uploadSingleFile(mockMultipartFile);
        uploadedFiles.add(savedFileName); // 정리를 위해 추가

        // then: 업로드 검증
        assertNotNull(savedFileName, "파일 업로드에 실패했습니다.");
        assertTrue(fileUtil.fileExists(savedFileName), "업로드된 파일이 존재하지 않습니다.");
    }

    @Test
    @Order(2)
    @DisplayName("업로드된 파일을 다운로드 받을 수 있어야 한다")
    void downloadFile_ShouldReturnCorrectFileContent() throws Exception {
        // given: 파일 업로드
        MockMultipartFile mockMultipartFile = createTestFile();
        savedFileName = fileUtil.uploadSingleFile(mockMultipartFile);
        uploadedFiles.add(savedFileName);

        // when: 파일 다운로드
        byte[] downloaded = fileUtil.downloadFile(savedFileName);

        // then: 파일 내용 검증
        assertNotNull(downloaded, "파일 다운로드에 실패했습니다.");
        assertEquals(TEST_FILE_CONTENT, new String(downloaded, StandardCharsets.UTF_8),
                "다운로드한 파일 내용이 다릅니다.");
    }

    @Test
    @Order(3)
    @DisplayName("업로드된 파일을 삭제할 수 있어야 한다")
    void deleteFile_ShouldRemoveUploadedFile() throws Exception {
        // given: 파일 업로드
        MockMultipartFile mockMultipartFile = createTestFile();
        savedFileName = fileUtil.uploadSingleFile(mockMultipartFile);
        assertTrue(fileUtil.fileExists(savedFileName), "파일이 업로드되어야 테스트가 유효합니다.");

        // when: 파일 삭제
        boolean deleted = fileUtil.deleteFile(savedFileName);

        // then: 삭제 검증
        assertTrue(deleted, "파일 삭제에 실패했습니다.");
        assertFalse(fileUtil.fileExists(savedFileName), "파일이 여전히 존재합니다.");
    }

    @Test
    @Order(4)
    @DisplayName("존재하지 않는 파일 다운로드 시 예외가 발생해야 한다")
    void downloadNonExistentFile_ShouldThrowException() {
        // given: 존재하지 않는 파일명
        String nonExistentFileName = "non_existent_file.txt";

        // when & then: 예외 발생 검증
        assertThrows(Exception.class, () -> {
            fileUtil.downloadFile(nonExistentFileName);
        }, "존재하지 않는 파일 다운로드 시 예외가 발생해야 합니다.");
    }

    @Test
    @Order(5)
    @DisplayName("업로드된 파일의 존재를 확인할 수 있어야 한다")
    void fileExists_ShouldConfirmFileExistence() throws Exception {
        // given: 파일 업로드
        MockMultipartFile mockMultipartFile = createTestFile();
        savedFileName = fileUtil.uploadSingleFile(mockMultipartFile);
        uploadedFiles.add(savedFileName);

        // when & then: 파일 존재 확인
        assertTrue(fileUtil.fileExists(savedFileName), "업로드된 파일이 존재해야 합니다.");
        assertFalse(fileUtil.fileExists("non_existent_file.txt"), "존재하지 않는 파일은 false를 반환해야 합니다.");
    }
}