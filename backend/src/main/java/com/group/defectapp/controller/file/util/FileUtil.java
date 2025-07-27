
package com.group.defectapp.controller.file.util;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FileUtil {

    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String EXCEL_CONTENT_TYPE_PREFIX = "application/vnd.ms-excel";
    private static final String EXCEL_OPENXML_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String DOC_CONTENT_TYPE_PREFIX = "application/msword";
    private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String PPT_CONTENT_TYPE_PREFIX = "application/vnd.ms-powerpoint";
    private static final String PPTX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    private static final String TEXT_CONTENT_TYPE_PREFIX = "text/";
    private static final String ZIP_CONTENT_TYPE = "application/zip";
    private static final String RAR_CONTENT_TYPE = "application/x-rar-compressed";
    private static final String GZIP_CONTENT_TYPE = "application/gzip";
    private static final String SEVENZIP_CONTENT_TYPE = "application/x-7z-compressed";

    @Value("${defect.upload.path}")
    private String uploadPath;

    private String absoluteUploadPath;

    /**
     * Bean 초기화 시 업로드 경로를 설정합니다.
     */
    @PostConstruct
    public void init() {
        try {
            // 업로드 디렉토리가 존재하지 않으면 생성
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (Exception e) {
            log.error("업로드 경로 초기화 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("파일 업로드 경로 초기화 실패", e);
        }
    }

    /**
     * 업로드 경로를 반환합니다.
     *
     * @return 파일이 저장되는 절대 경로
     */
    public String getUploadPath() {
        return this.absoluteUploadPath != null ? this.absoluteUploadPath : this.uploadPath;
    }

    /**
     * 다수 파일을 업로드합니다.
     *
     * @param files 업로드할 파일 배열
     * @return 저장된 파일명 목록
     */
    public List<String> upload(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }

        return Arrays.stream(files)
                .filter(this::isSupportedFile)
                .map(this::uploadSingleFile)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 단일 파일을 업로드합니다.
     *
     * @param file 업로드할 파일
     * @return 저장된 파일명 또는 실패 시 null
     */
    public String uploadSingleFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (!isSupportedFile(file)) {
            log.error("지원되지 않는 파일 형식입니다: {}", file.getContentType());
            return null;
        }

        String saveFileName = generateSaveFileName(file);
        Path targetPath = getTargetPath(saveFileName);

        try {
            // 디렉토리가 존재하지 않으면 생성
            Files.createDirectories(targetPath.getParent());

            try (InputStream in = file.getInputStream();
                 OutputStream out = new FileOutputStream(targetPath.toFile())) {
                FileCopyUtils.copy(in, out);
                log.info("파일이 성공적으로 저장되었습니다: {}", saveFileName);
                return saveFileName;
            }
        } catch (IOException e) {
            log.error("파일 업로드에 실패했습니다 {}: {}", file.getOriginalFilename(), e.getMessage());
            return null;
        }
    }

    /**
     * 파일을 삭제합니다.
     *
     * @param fileName 삭제할 파일명
     * @return 삭제 성공 여부
     */
    public boolean deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        Path filePath = getTargetPath(fileName);
        File file = filePath.toFile();

        try {
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    log.info("파일이 성공적으로 삭제되었습니다: {}", fileName);
                } else {
                    log.warn("파일 삭제에 실패했습니다: {}", fileName);
                }
                return deleted;
            }
            return false;
        } catch (Exception e) {
            log.error("파일 삭제 중 오류 발생 {}: {}", fileName, e.getMessage());
            return false;
        }
    }

    public byte[] downloadFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        Path filePath = getTargetPath(fileName);
        File file = filePath.toFile();

        try {
            if (!file.exists() || !file.isFile()) {
                log.error("파일이 존재하지 않습니다: {}", fileName);
                return null;
            }

            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("파일 다운로드 중 오류 발생 {}: {}", fileName, e.getMessage());
            return null;
        }
    }

    /**
     * 파일이 존재하는지 확인합니다.
     *
     * @param fileName 확인할 파일명
     * @return 파일 존재 여부
     */
    public boolean fileExists(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        Path filePath = getTargetPath(fileName);
        File file = filePath.toFile();

        return file.exists() && file.isFile();
    }

    /**
     * 파일의 원본 이름을 추출합니다.
     *
     * @param savedFileName 저장된 파일명(UUID가 포함된)
     * @return 원본 파일명
     */
    public String getOriginalFileName(String savedFileName) {
        if (savedFileName == null || !savedFileName.contains("_")) {
            return savedFileName;
        }

        // UUID_originalFileName 형식에서 원본 파일명 추출
        int underscoreIndex = savedFileName.indexOf("_");
        if (underscoreIndex > 0 && underscoreIndex < savedFileName.length() - 1) {
            return savedFileName.substring(underscoreIndex + 1);
        }

        return savedFileName;
    }

    /**
     * 파일이 지원되는 타입인지 확인합니다.
     *
     * @param file 확인할 파일
     * @return 지원되는 파일 타입 여부
     */
    private boolean isSupportedFile(MultipartFile file) {
        if (file == null || file.getContentType() == null) {
            return false;
        }

        String contentType = file.getContentType();

        return isImageFile(contentType) ||
                isDocumentFile(contentType) ||
                isArchiveFile(contentType);
    }

    /**
     * 이미지 파일인지 확인합니다.
     *
     * @param contentType 파일의 MIME 타입
     * @return 이미지 파일 여부
     */
    private boolean isImageFile(String contentType) {
        return contentType.startsWith(IMAGE_CONTENT_TYPE_PREFIX);
    }

    /**
     * 문서 파일인지 확인합니다.
     *
     * @param contentType 파일의 MIME 타입
     * @return 문서 파일 여부
     */
    private boolean isDocumentFile(String contentType) {
        return contentType.equals(PDF_CONTENT_TYPE) ||
                contentType.startsWith(EXCEL_CONTENT_TYPE_PREFIX) ||
                contentType.equals(EXCEL_OPENXML_CONTENT_TYPE) ||
                contentType.startsWith(DOC_CONTENT_TYPE_PREFIX) ||
                contentType.equals(DOCX_CONTENT_TYPE) ||
                contentType.startsWith(PPT_CONTENT_TYPE_PREFIX) ||
                contentType.equals(PPTX_CONTENT_TYPE) ||
                contentType.startsWith(TEXT_CONTENT_TYPE_PREFIX);
    }

    /**
     * 압축 파일인지 확인합니다.
     *
     * @param contentType 파일의 MIME 타입
     * @return 압축 파일 여부
     */
    private boolean isArchiveFile(String contentType) {
        return contentType.equals(ZIP_CONTENT_TYPE) ||
                contentType.equals(RAR_CONTENT_TYPE) ||
                contentType.equals(GZIP_CONTENT_TYPE) ||
                contentType.equals(SEVENZIP_CONTENT_TYPE);
    }

    private String generateSaveFileName(MultipartFile file) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return timestamp + "_" + file.getOriginalFilename();
    }

    private Path getTargetPath(String fileName) {
        return Paths.get(uploadPath, fileName);
    }
}