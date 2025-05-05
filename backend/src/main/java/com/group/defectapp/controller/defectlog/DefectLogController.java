package com.group.defectapp.controller.defectlog;


import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.defectlog.DefectLogListDto;
import com.group.defectapp.dto.defectlog.DefectLogRequestDto;
import com.group.defectapp.service.defectlog.DefectLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/defectLogs")
@RequiredArgsConstructor
public class DefectLogController {

    private final DefectLogService defectLogService;

    @PostMapping("/save")
    public ResponseEntity<Void> saveDefectLog(@Valid @RequestBody DefectLogRequestDto defectLogRequestDto, @RequestParam(required = false) MultipartFile[] files) {
        defectLogService.defectLogSave(defectLogRequestDto, files);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list/{defectId}")
    public ResponseEntity<Page<DefectLogListDto>> listDefectLogs(@Validated PageRequestDto pageRequestDto, @PathVariable String defectId) {
        Page<DefectLogListDto> defectLogList = defectLogService.defectLogList(pageRequestDto, defectId);
        return ResponseEntity.ok(defectLogList);
    }
}
