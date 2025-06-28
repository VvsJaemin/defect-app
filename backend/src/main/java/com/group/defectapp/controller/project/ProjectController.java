package com.group.defectapp.controller.project;


import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.project.ProjectRequestDto;
import com.group.defectapp.dto.project.ProjectResponseDto;
import com.group.defectapp.dto.project.ProjectUserListDto;
import com.group.defectapp.service.project.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('CU', 'DM', 'DP', 'MG', 'QA')")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/save")
    public ResponseEntity<String> saveProject(@Valid @RequestBody ProjectRequestDto projectRequestDto) {
        projectService.saveProject(projectRequestDto);
        return ResponseEntity.ok("프로젝트 등록이 성공했습니다.");
    }

    @GetMapping("/list")
    public ResponseEntity<Page<ProjectResponseDto>> listProjects(@Validated PageRequestDto pageRequestDto,
                                                                @RequestParam(required = false) Map<String,Object> paramMap) {
        Page<ProjectResponseDto> projectResponseList = projectService.getProjectsList(pageRequestDto, paramMap);
        return ResponseEntity.ok(projectResponseList);
    }

    @GetMapping("/read")
    public ResponseEntity<ProjectResponseDto> readProject(@RequestParam String projectId) {
        ProjectResponseDto projectResponseDto = projectService.readProject(projectId);
        return ResponseEntity.ok(projectResponseDto);
    }

    @PostMapping("/modify-projects")
    public ResponseEntity<String> modifyProject(@Valid @RequestBody ProjectRequestDto projectRequestDto) {
        projectService.updateProject(projectRequestDto);
        return ResponseEntity.ok("프로젝트 수정이 성공했습니다.");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteProject(@RequestBody List<String> projectIds) {
        projectService.deleteProjects(projectIds);
        return ResponseEntity.ok("등록하신 프로젝트를 정상적으로 삭제했습니다.");
    }

    @GetMapping("/assignUserList")
    public ResponseEntity<List<ProjectUserListDto>> assignUserList(@RequestParam(required = false) String projectId) {
        List<ProjectUserListDto> userList = projectService.assignProjectUserList(projectId);
        return ResponseEntity.ok(userList);
    }

}

