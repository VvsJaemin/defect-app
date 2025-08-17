
package com.group.defectapp.service.project;

import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.domain.project.ProjectAssignUser;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.common.PageRequestDto;
import com.group.defectapp.dto.project.ProjectRequestDto;
import com.group.defectapp.dto.project.ProjectResponseDto;
import com.group.defectapp.dto.project.ProjectSearchCondition;
import com.group.defectapp.dto.project.ProjectUserListDto;
import com.group.defectapp.exception.project.ProjectCode;
import com.group.defectapp.repository.cmCode.CommonCodeRepository;
import com.group.defectapp.repository.defect.DefectRepository;
import com.group.defectapp.repository.defectlog.DefectLogRepository;
import com.group.defectapp.repository.project.ProjectRepository;
import com.group.defectapp.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final DefectRepository defectRepository;
    private final DefectLogRepository defectLogRepository;

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

        Optional.ofNullable(projectRequestDto.getProjAssignedUsers())
                .filter(users -> !users.isEmpty())
                .ifPresent(users -> users.forEach(userId -> {
                    ProjectAssignUser assignUser = ProjectAssignUser.builder()
                            .projectId(newProjectId)
                            .userId(userId)
                            .build();
                    project.addProjAssignedUser(assignUser);
                }));
    }

    public Page<ProjectResponseDto> getProjectsList(PageRequestDto pageRequestDto, Map<String, Object> paramMap) {
        // 개선된 파라미터 추출 - 불필요한 변수 할당 제거
        ProjectSearchCondition condition = ProjectSearchCondition.builder()
                .projectName((String) paramMap.getOrDefault("projectName", null))
                .customerName((String) paramMap.getOrDefault("customerName", null))
                .urlInfo((String) paramMap.getOrDefault("urlInfo", null))
                .projectState((String) paramMap.getOrDefault("projectState", null))
                .build();

        PageRequestDto finalPageRequest = PageRequestDto.builder()
                .pageIndex(Integer.parseInt(Objects.toString(paramMap.get("pageIndex"))))
                .pageSize(Integer.parseInt(Objects.toString(paramMap.get("pageSize"))))
                .sortKey(Objects.toString(paramMap.get("sortKey")))
                .sortOrder(Objects.toString(paramMap.get("sortOrder")))
                .build();

        return projectRepository.list(finalPageRequest.getPageable(), condition);
    }

    public ProjectResponseDto readProject(String projectId) {
        Project project = projectRepository.findByProjectId(projectId)
                .orElseThrow(ProjectCode.PROJECT_NOT_FOUND::getProjectException);

        Set<String> assignedUserIds = project.getProjAssignedUsers();
        Map<String, String> userIdToNameMap = Optional.ofNullable(assignedUserIds)
                .filter(ids -> !ids.isEmpty())
                .map(ids -> userRepository.findByUserIdIn(ids).stream()
                        .collect(Collectors.toMap(User::getUserId, User::getUserName)))
                .orElse(Collections.emptyMap());

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
                .assignedUsersMap(userIdToNameMap)
                .build();
    }

    @Transactional
    public void updateProject(ProjectRequestDto projectRequestDto) {
        Project project = projectRepository.findByProjectId(projectRequestDto.getProjectId())
                .orElseThrow(ProjectCode.PROJECT_NOT_FOUND::getProjectException);

        // 프로젝트 기본 정보 업데이트
        project.updateProjectInfo(
                projectRequestDto.getProjectName(),
                projectRequestDto.getUrlInfo(),
                projectRequestDto.getCustomerName(),
                projectRequestDto.getStatusCode(),
                projectRequestDto.getEtcInfo(),
                projectRequestDto.getUseYn(),
                projectRequestDto.getUpdatedBy()
        );

        project.getProjAssignedUsers().clear();

        Optional.ofNullable(projectRequestDto.getProjAssignedUsers())
                .filter(users -> !users.isEmpty())
                .ifPresent(users -> users.forEach(userId -> {
                    ProjectAssignUser assignUser = ProjectAssignUser.builder()
                            .projectId(project.getProjectId())
                            .userId(userId)
                            .build();
                    project.addProjAssignedUser(assignUser);
                }));
    }

    @Transactional
    public void deleteProject(String projectId) {
        Project project = projectRepository.findByProjectId(projectId)
                .orElseThrow(ProjectCode.PROJECT_NOT_FOUND::getProjectException);

        try {
            projectRepository.delete(project);
        } catch (Exception e) {
            log.error("프로젝트 삭제 중 오류 발생: projectId={}", projectId, e);
            throw ProjectCode.PROJECT_NOT_REMOVED.getProjectException();
        }
    }

    @Transactional
    public void deleteProjects(List<String> projectIds) {
        if (ObjectUtils.isEmpty(projectIds)) {
            log.warn("삭제할 프로젝트 ID 목록이 비어있습니다.");
            return;
        }

        // 1. 결함 로그 파일 삭제 (log_seq 기준)
        defectLogRepository.deleteAllDefectLogFileByProjectIdIn(projectIds);

        // 2. 결함 로그 내역 삭제
        defectLogRepository.deleteAllByProjectIdIn(projectIds);

        // 3. 결함 삭제
        defectRepository.deleteAllByProjectIdIn(projectIds);

        // 4. 프로젝트 삭제
        projectRepository.deleteAllByIdIn(projectIds);
    }


    public List<ProjectUserListDto> assignProjectUserList(String projectId) {
        if (StringUtils.hasText(projectId)) {
            Set<String> assignedUserIds = projectRepository.findAssignedUserIdsByProjectId(projectId);

            return assignedUserIds.isEmpty() ? Collections.emptyList() :
                    userRepository.findByUserIdIn(assignedUserIds).stream()
                            .map(this::convertToProjectUserListDto)
                            .collect(Collectors.toList());
        } else {
            return userRepository.findAll().stream()
                    .map(this::convertToProjectUserListDto)
                    .collect(Collectors.toList());
        }
    }

    private ProjectUserListDto convertToProjectUserListDto(User user) {
        return ProjectUserListDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .userSeCd(user.getUserSeCd())
                .build();
    }

    private User findAssigneeIfExists(String assigneeId) {
        return StringUtils.hasText(assigneeId) ?
                userRepository.findByUserId(assigneeId)
                        .orElseThrow(ProjectCode.PROJECT_WRITER_ERROR::getProjectException) :
                null;
    }
}