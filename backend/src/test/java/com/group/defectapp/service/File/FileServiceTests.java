package com.group.defectapp.service.File;

import com.group.defectapp.controller.file.util.FileUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FileServiceTests {

    @Autowired
    private FileUtil fileUtil;

    @Value("${defect.upload.path}")
    private String uploadPath;

    private String savedFileName;

    @Test
    void 파일_업로드_다운로드_삭제_테스트() throws Exception {
        // given: 테스트용 업로드 파일 생성 (text/plain 지원됨)
        String testFileName = "test.txt";
        String fileContent = "테스트 파일 내ㄴㄴ용입니다.";
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "file",
                testFileName,
                "text/plain",
                fileContent.getBytes(StandardCharsets.UTF_8)
        );

        // when: 파일 업로드
        savedFileName = fileUtil.uploadSingleFile(mockMultipartFile);

        // then: 저장 파일명, 파일 존재 여부 확인
        assertNotNull(savedFileName, "파일 업로드에 실패했습니다.");
        assertTrue(fileUtil.fileExists(savedFileName), "업로드된 파일이 존재하지 않습니다.");

        // when: 파일 다운로드
        byte[] downloaded = fileUtil.downloadFile(savedFileName);

        // then: 파일 내용 확인
        assertNotNull(downloaded, "파일 다운로드에 실패했습니다.");
        assertEquals(fileContent, new String(downloaded, StandardCharsets.UTF_8), "다운로드한 파일 내용이 다릅니다.");

        // when: 파일 삭제
        boolean deleted = fileUtil.deleteFile(savedFileName);

        // then: 파일 삭제 성공 및 존재하지 않음 확인
        assertTrue(deleted, "파일 삭제에 실패했습니다.");
        assertFalse(fileUtil.fileExists(savedFileName), "파일이 여전히 존재합니다.");
    }
}