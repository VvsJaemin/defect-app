package com.group.defectapp.service.project;


import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.domain.project.ProjectAssignUser;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.project.ProjectRequestDto;
import com.group.defectapp.dto.project.ProjectResponseDto;
import com.group.defectapp.dto.project.ProjectSearchCondition;
import com.group.defectapp.exception.project.ProjectCode;
import com.group.defectapp.repository.cmCode.CommonCodeRepository;
import com.group.defectapp.repository.project.ProjectRepository;
import com.group.defectapp.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CommonCodeRepository commonCodeRepository;

    @Transactional
    public void saveProject(ProjectRequestDto projectRequestDto) {

        User assignee = findAssigneeIfExists(projectRequestDto.getAssigneeId());

        CommonCode commonCode = commonCodeRepository.findBySeCode(projectRequestDto.getStatusCode())
                .orElseThrow(ProjectCode.PROJECT_NOT_REGISTERED::getProjectException);


        String newProjectId = projectRepository.generateProjectIdUsingSequence();

        Project project = projectRequestDto.toEntity(assignee, newProjectId, commonCode);

        // 프로젝트 저장
        projectRepository.save(project);

        // 할당된 사용자 처리
        if (projectRequestDto.getProjAssignedUsers() != null && !projectRequestDto.getProjAssignedUsers().isEmpty()) {
            for (String userId : projectRequestDto.getProjAssignedUsers()) {
                ProjectAssignUser assignUser = ProjectAssignUser.builder()
                        .projectId(newProjectId)
                        .userId(userId)
                        .build();
                project.addProjAssignedUser(assignUser);
            }
        }
    }

    public Page<ProjectResponseDto> getProjectsList(PageRequestDto pageRequestDto, Map<String, Object> paramMap) {

        ProjectSearchCondition condition = ProjectSearchCondition.builder()
                .searchType((String) paramMap.get("searchType"))
                .searchText((String) paramMap.get("searchText"))
                .build();

        Pageable pageable = pageRequestDto.getPageable(Sort.by("createdAt").descending());
        return projectRepository.list(pageable, condition);
    }

    public ProjectResponseDto readProject(String projectId) {
        Project project = projectRepository.findByProjectId(projectId)
                .orElseThrow(ProjectCode.PROJECT_NOT_FOUND::getProjectException);

        // 빌더 패턴으로 응답 DTO 생성
        return ProjectResponseDto.builder()
                .projectId(project.getProjectId())
                .projectName(project.getProjectName())
                .urlInfo(project.getUrlInfo())
                .customerName(project.getCustomerName())
                .statusCode(project.getStatusCode())
                .etcInfo(project.getEtcInfo())
                .useYn(project.getUseYn())
                .createdAt(project.getCreatedAt())
                .createdBy(project.getCreatedBy())
                .updatedAt(project.getUpdatedAt())
                .updatedBy(project.getUpdatedBy())
                .assignedUsers(project.getProjAssignedUsers())
                .build();
    }


    @Transactional
    public void updateProject(ProjectRequestDto projectRequestDto) {
        Project project = projectRepository.findByProjectId(projectRequestDto.getProjectId())
                .orElseThrow(ProjectCode.PROJECT_NOT_FOUND::getProjectException);

        // 작성자 권한 확인 로직이 필요할 경우 여기에 추가
        // 예: if (!project.getCreatedBy().equals(projectRequestDto.getUpdatedBy()))
        //     throw ProjectCode.PROJECT_NOT_MODIFIED.getProjectException();


        project.updateProjectInfo(
                projectRequestDto.getProjectName(),
                projectRequestDto.getUrlInfo(),
                projectRequestDto.getCustomerName(),
                projectRequestDto.getStatusCode(),
                projectRequestDto.getEtcInfo(),
                projectRequestDto.getUseYn(),
                projectRequestDto.getUpdatedBy()
        );

        // 할당된 사용자 업데이트 (기존 할당자를 모두 제거하고 새로 추가)
        project.getProjAssignedUsers().clear();

        if (projectRequestDto.getProjAssignedUsers() != null && !projectRequestDto.getProjAssignedUsers().isEmpty()) {
            for (String userId : projectRequestDto.getProjAssignedUsers()) {
                ProjectAssignUser assignUser = ProjectAssignUser.builder()
                        .projectId(project.getProjectId())
                        .userId(userId)
                        .build();
                project.addProjAssignedUser(assignUser);
            }
        }
    }

    @Transactional
    public void deleteProject(String projectId) {
        Project project = projectRepository.findByProjectId(projectId)
                .orElseThrow(ProjectCode.PROJECT_NOT_FOUND::getProjectException);

        // 작성자 권한 확인 로직이 필요할 경우 여기에 추가
        // 예: if (!project.getCreatedBy().equals(currentUserId))
        //     throw ProjectCode.PROJECT_WRITER_ERROR.getProjectException();

        try {
            projectRepository.delete(project);
        } catch (Exception e) {
            log.error("프로젝트 삭제 중 오류 발생", e);
            throw ProjectCode.PROJECT_NOT_REMOVED.getProjectException();
        }
    }


    private User findAssigneeIfExists(String assigneeId) {
        if (assigneeId == null || assigneeId.isEmpty()) {
            return null;
        }
        return userRepository.findByUserId(assigneeId)
                .orElseThrow(ProjectCode.PROJECT_WRITER_ERROR::getProjectException);

    }
}
