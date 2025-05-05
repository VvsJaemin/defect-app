package com.group.defectapp.controller.file;

import com.group.defectapp.controller.file.util.FileUtil;
import com.group.defectapp.exception.file.FileCode;
import com.group.defectapp.exception.file.FileNotSupportedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    /**
     * 허용되는 이미지 파일 확장자 정규식
     */
    private static final String ALLOWED_IMAGE_EXTENSIONS = "^(jpg|jpeg|JPG|JPEG|png|PNG|gif|GIF|bmp|BMP)$";

    /**
     * 허용되는 문서 파일 확장자 정규식
     */
    private static final String ALLOWED_DOCUMENT_EXTENSIONS = "^(pdf|PDF|doc|DOC|docx|DOCX|ppt|PPT|pptx|PPTX|xls|XLS|xlsx|XLSX|txt|TXT)$";

    /**
     * 허용되는 압축 파일 확장자 정규식
     */
    private static final String ALLOWED_ARCHIVE_EXTENSIONS = "^(zip|ZIP|rar|RAR|7z|tar|TAR|gz|GZ)$";

    private final FileUtil fileUtil;

    /**
     * 파일 업로드 API
     *
     * @param files 업로드할 파일 배열
     * @return 저장된 파일명 목록
     */
    @PostMapping("/upload")
    public ResponseEntity<List<String>> uploadFile(@RequestParam("files") MultipartFile[] files) {
        log.info("파일 업로드 요청 수신");

        if (files == null || files.length == 0) {
            throw FileCode.UPLOAD_FILE_NOT_SUPPORTED.getFileNotSupportedException();
        }

        // 모든 파일 타입 검증
        for (MultipartFile file : files) {
            validateFileType(file.getOriginalFilename());
        }

        // 파일 업로드 실행
        List<String> result = fileUtil.upload(files);

        log.info("파일 업로드 완료: {} 파일", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileName) {
        if (!fileUtil.fileExists(fileName)) {
            return ResponseEntity.notFound().build();
        }

        byte[] fileData = fileUtil.downloadFile(fileName);
        if (fileData == null) {
            return ResponseEntity.internalServerError().build();
        }

        String originalFileName = fileUtil.getOriginalFileName(fileName);
        String encodedFileName = UriUtils.encode(originalFileName, StandardCharsets.UTF_8);
        String contentDisposition = "attachment; filename=\"" + encodedFileName + "\"";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(fileData);
    }


    /**
     * 파일 확장자 유효성 검사
     *
     * @param fileName 검사할 파일명
     * @throws FileNotSupportedException 지원하지 않는 파일 형식인 경우
     */
    private void validateFileType(String fileName) throws FileNotSupportedException {
        if (fileName == null || !fileName.contains(".")) {
            throw FileCode.FILE_NOT_FOUND.getFileNotSupportedException();
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

        // 이미지, 문서, 압축 파일 확장자 체크
        if (extension.matches(ALLOWED_IMAGE_EXTENSIONS)
                || extension.matches(ALLOWED_DOCUMENT_EXTENSIONS)
                || extension.matches(ALLOWED_ARCHIVE_EXTENSIONS)) {
            return; // 유효한 파일 타입
        }

        throw FileCode.FILE_FORMAT_NOT_SUPPORTED.getFileNotSupportedException();

    }

    /**
     * 이미지 파일 확장자 유효성 검사
     *
     * @param fileName 검사할 파일명
     * @throws FileNotSupportedException 지원하지 않는 파일 형식인 경우
     */
    private void validateImageFileType(String fileName) throws FileNotSupportedException {
        if (fileName == null || !fileName.contains(".")) {
            throw FileCode.FILE_NOT_FOUND.getFileNotSupportedException();
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

        if (!extension.matches(ALLOWED_IMAGE_EXTENSIONS)) {
            throw FileCode.FILE_FORMAT_NOT_SUPPORTED.getFileNotSupportedException();
        }
    }

}