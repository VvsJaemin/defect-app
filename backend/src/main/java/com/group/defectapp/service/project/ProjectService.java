package com.group.defectapp.service.project;

import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.project.ProjectRequestDto;
import com.group.defectapp.dto.project.ProjectResponseDto;
import com.group.defectapp.dto.project.ProjectUserListDto;
import com.group.defectapp.dto.user.UserListDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ProjectService {

    /**
     * 프로젝트를 저장합니다.
     *
     * @param projectRequestDto 프로젝트 저장 요청 DTO
     */
    void saveProject(ProjectRequestDto projectRequestDto);

    /**
     * 조건에 맞는 프로젝트 목록을 페이징하여 조회합니다.
     *
     * @param pageRequestDto 페이지 요청 DTO
     * @param paramMap 검색 조건 정보 맵
     * @return 프로젝트 목록 페이지
     */
    Page<ProjectResponseDto> getProjectsList(PageRequestDto pageRequestDto, Map<String, Object> paramMap);

    /**
     * 특정 프로젝트의 상세 정보를 조회합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 프로젝트 상세 정보 DTO
     */
    ProjectResponseDto readProject(String projectId);

    /**
     * 기존 프로젝트 정보를 수정합니다.
     *
     * @param projectRequestDto 프로젝트 수정 요청 DTO
     */
    void updateProject(ProjectRequestDto projectRequestDto);

    /**
     * 프로젝트를 삭제합니다.
     *
     * @param projectId 삭제할 프로젝트 ID
     */
    void deleteProject(String projectId);

    void deleteProjects(List<String> projectIds);


    List<ProjectUserListDto> assignProjectUserList();


}