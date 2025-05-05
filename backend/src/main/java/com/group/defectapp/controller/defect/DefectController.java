package com.group.defectapp.controller.defect;

import com.group.defectapp.dto.defect.DefectListDto;
import com.group.defectapp.dto.defect.DefectRequestDto;
import com.group.defectapp.dto.defect.DefectResponseDto;
import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.service.defect.DefectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/defects")
@RequiredArgsConstructor
public class DefectController {

    private final DefectService defectService;

    @PostMapping("/save")
    public ResponseEntity<String> saveDefect(@Valid @RequestBody DefectRequestDto defectRequestDto,
                                             @RequestParam(required = false) MultipartFile[] files,
                                             Principal principal) {
        String loginUserId = principal.getName();
        defectService.saveDefect(defectRequestDto, files, loginUserId);
        return ResponseEntity.ok("결함 등록이 성공했습니다.");
    }

    @GetMapping("/list")
    public ResponseEntity<Page<DefectListDto>> listDefects(@Validated PageRequestDto pageRequestDto,
                                                           @RequestParam(required = false) Map<String, Object> paramMap) {
        Page<DefectListDto> defectResponseList = defectService.defectList(pageRequestDto, paramMap);
        return ResponseEntity.ok(defectResponseList);
    }

    @GetMapping("/read/{defectId}")
    public ResponseEntity<DefectResponseDto> readDefect(@PathVariable String defectId) {
        DefectResponseDto defectResponseDto = defectService.readDefect(defectId);
        return ResponseEntity.ok(defectResponseDto);
    }

    @PutMapping("/modify-defects")
    public ResponseEntity<String> modifyDefect(@Valid @RequestBody DefectRequestDto defectRequestDto,
                                               @RequestParam(required = false) MultipartFile[] files,
                                               Principal principal) {
        defectService.modifyDefect(defectRequestDto, files, principal);
        return ResponseEntity.ok("결함 수정이 성공했습니다.");
    }

    @DeleteMapping("/{defectId}")
    public ResponseEntity<String> deleteDefect(@PathVariable String defectId, Principal principal) {
        defectService.deleteDefect(defectId, principal);
        return ResponseEntity.ok("등록하신 결함을 정상적으로 삭제했습니다.");
    }
}
