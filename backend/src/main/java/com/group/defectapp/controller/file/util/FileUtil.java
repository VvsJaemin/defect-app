package com.group.defectapp.controller.file.util;

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
    private static final String DOC_CONTENT_TYPE = "application/msword";
    private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String XLS_CONTENT_TYPE = "application/vnd.ms-excel";
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Getter
    @Value("${defect.upload.path}")
    private String uploadPath;


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


    public String uploadSingleFile(MultipartFile file) {
        if (file == null || file.isEmpty() || !isSupportedFile(file)) {
            return null;
        }

        String saveFileName = generateSaveFileName(file);
        Path targetPath = getTargetPath(saveFileName);

        try (InputStream in = file.getInputStream();
             OutputStream out = new FileOutputStream(targetPath.toFile())) {
            FileCopyUtils.copy(in, out);
            return saveFileName;
        } catch (IOException e) {
            log.error("파일 업로드 실패 - 파일명: {}, 오류: {}", file.getOriginalFilename(), e.getMessage());
            return null;
        }
    }


    public boolean deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        Path filePath = getTargetPath(fileName);
        File file = filePath.toFile();

        try {
            return file.exists() && file.delete();
        } catch (Exception e) {
            log.error("파일 삭제 실패 - 파일명: {}, 오류: {}", fileName, e.getMessage());
            return false;
        }
    }


    public byte[] downloadFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        Path filePath = getTargetPath(fileName);
        File file = filePath.toFile();

        if (!file.exists() || !file.isFile()) {
            return null;
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("파일 다운로드 실패 - 파일명: {}, 오류: {}", fileName, e.getMessage());
            return null;
        }
    }


    public boolean fileExists(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        Path filePath = getTargetPath(fileName);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }


    public String getOriginalFileName(String savedFileName) {
        if (savedFileName == null || !savedFileName.contains("_")) {
            return savedFileName;
        }

        int underscoreIndex = savedFileName.indexOf("_");
        return underscoreIndex > 0 && underscoreIndex < savedFileName.length() - 1
                ? savedFileName.substring(underscoreIndex + 1)
                : savedFileName;
    }

    private boolean isSupportedFile(MultipartFile file) {
        if (file == null || file.getContentType() == null) {
            return false;
        }

        String contentType = file.getContentType();
        return isImageFile(contentType) || isDocumentFile(contentType);
    }

    private boolean isImageFile(String contentType) {
        return contentType.startsWith(IMAGE_CONTENT_TYPE_PREFIX);
    }

    private boolean isDocumentFile(String contentType) {
        return contentType.equals(PDF_CONTENT_TYPE) ||
                contentType.equals(DOC_CONTENT_TYPE) ||
                contentType.equals(DOCX_CONTENT_TYPE) ||
                contentType.equals(XLS_CONTENT_TYPE) ||
                contentType.equals(XLSX_CONTENT_TYPE);
    }

    private String generateSaveFileName(MultipartFile file) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return timestamp + "_" + file.getOriginalFilename();
    }

    private Path getTargetPath(String fileName) {
        return Paths.get(uploadPath, fileName);
    }
}