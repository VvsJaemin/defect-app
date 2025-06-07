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

        // 원본 파일명이 null이거나 비어있는 경우 시스템 파일명 사용
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            originalFileName = fileName;
        }

        // UTF-8로 인코딩된 파일명 설정 (브라우저 호환성을 위해 두 가지 방식 모두 제공)
        String encodedFileName = UriUtils.encode(originalFileName, StandardCharsets.UTF_8);
        String contentDisposition = String.format(
                "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                originalFileName.replaceAll("[\"\\\\]", "_"), // 특수문자 제거
                encodedFileName
        );

        // MIME 타입 감지 및 설정
        String contentType = determineContentType(originalFileName);

        log.info("파일 다운로드: {} -> {}", fileName, originalFileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(fileData);
    }


    /**
     * 파일 확장자를 기반으로 MIME 타입을 결정합니다.
     *
     * @param fileName 파일명
     * @return MIME 타입
     */
    private String determineContentType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "application/octet-stream";
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        switch (extension) {
            // 이미지 파일
            case "jpg":
                return "image/jpeg";
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            
            // 문서 파일
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt":
                return "text/plain";
            
            // 압축 파일
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            case "7z":
                return "application/x-7z-compressed";
            case "tar":
                return "application/x-tar";
            case "gz":
                return "application/gzip";
                
            default:
                return "application/octet-stream";
        }
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