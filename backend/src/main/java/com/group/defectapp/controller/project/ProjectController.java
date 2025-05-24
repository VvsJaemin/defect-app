package com.group.defectapp.controller.project;


import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.project.ProjectRequestDto;
import com.group.defectapp.dto.project.ProjectResponseDto;
import com.group.defectapp.service.project.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('CU', 'DM', 'DP', 'MG', 'QA')")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/save")
    public ResponseEntity<String> saveDefect(@Valid @RequestBody ProjectRequestDto projectRequestDto) {
        projectService.saveProject(projectRequestDto);
        return ResponseEntity.ok("프로젝트 등록이 성공했습니다.");
    }

    @GetMapping("/list")
    public ResponseEntity<Page<ProjectResponseDto>> listDefects(@Validated PageRequestDto pageRequestDto,
                                                                @RequestParam(required = false) Map<String,Object> paramMap) {
        Page<ProjectResponseDto> defectResponseList = projectService.getProjectsList(pageRequestDto, paramMap);
        return ResponseEntity.ok(defectResponseList);
    }

    @GetMapping("/read/{projectId}")
    public ResponseEntity<ProjectResponseDto> readProject(@PathVariable String projectId) {
        ProjectResponseDto projectResponseDto = projectService.readProject(projectId);
        return ResponseEntity.ok(projectResponseDto);
    }

    @PostMapping("/modify-projects")
    public ResponseEntity<String> modifyProject(@Valid @RequestBody ProjectRequestDto projectRequestDto) {
        projectService.updateProject(projectRequestDto);
        return ResponseEntity.ok("프로젝트 수정이 성공했습니다.");
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<String> deleteProject(@PathVariable String projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.ok("등록하신 프로젝트를 정상적으로 삭제했습니다.");
    }
}

