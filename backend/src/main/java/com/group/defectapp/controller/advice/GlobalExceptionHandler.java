package com.group.defectapp.controller.advice;

import com.group.defectapp.exception.defect.DefectException;
import com.group.defectapp.exception.defectLog.DefectLogException;
import com.group.defectapp.exception.file.FileNotSupportedException;
import com.group.defectapp.exception.project.ProjectException;
import com.group.defectapp.exception.user.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage());

        List<ObjectError> errors = e.getBindingResult().getAllErrors();
        String errorMessage = errors.stream().map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest().body(Map.of("error", errorMessage));
    }

    @ExceptionHandler(DefectException.class)
    public ResponseEntity<Map<String, Object>> handleDefectException(DefectException e) {
        log.error(e.getClass().getName());
        log.error(e.getMessage());

        int status = e.getCode();

        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(ProjectException.class)
    public ResponseEntity<Map<String, Object>> handleProjectException(DefectException e) {
        log.error(e.getClass().getName());
        log.error(e.getMessage());

        int status = e.getCode();

        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(DefectLogException.class)
    public ResponseEntity<Map<String, Object>> handleDefectLogException(DefectLogException e) {
        log.error(e.getClass().getName());
        log.error(e.getMessage());

        int status = e.getCode();

        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<Map<String, Object>> handleUserException(UserException e) {
        log.error(e.getClass().getName());
        log.error(e.getMessage());

        int status = e.getCode();

        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(FileNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleFileNotSupportedException(FileNotSupportedException e) {
        log.error(e.getClass().getName());
        log.error(e.getMessage());

        int status = e.getCode();

        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }


    // 그 외 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("처리되지 않은 예외 발생: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "서버 내부 오류가 발생했습니다."));
    }
}
