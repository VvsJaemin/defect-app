package com.group.defectapp.controller.file;

import com.group.defectapp.controller.file.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileUtil fileUtil;

    @PostMapping("/upload")
    public ResponseEntity<List<String>> uploadFile(@RequestParam("files") MultipartFile[] files) {
        List<String> result = fileUtil.upload(files);
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

        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            originalFileName = fileName;
        }

        String encodedFileName = UriUtils.encode(originalFileName, StandardCharsets.UTF_8);
        String contentDisposition = String.format(
                "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                originalFileName.replaceAll("[\"\\\\]", "_"),
                encodedFileName
        );

        String contentType = determineContentType(originalFileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(fileData);
    }

    /**
     * 파일 확장자를 기반으로 MIME 타입을 결정합니다.
     */
    private String determineContentType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "application/octet-stream";
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        switch (extension) {
            // 이미지 파일
            case "jpg":
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
            case "tiff":
            case "tif":
                return "image/tiff";

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

            default:
                return "application/octet-stream";
        }
    }
}