package com.group.defectapp.repository.project.search;

import com.group.defectapp.domain.project.Project;
import com.group.defectapp.domain.project.QProject;
import com.group.defectapp.dto.project.ProjectResponseDto;
import com.group.defectapp.dto.project.ProjectSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class ProjectSearchImpl extends QuerydslRepositorySupport implements ProjectSearch {

    private final QProject qProject = QProject.project;
    
    public ProjectSearchImpl() {
        super(Project.class);
    }
    
    @Override
    public Page<ProjectResponseDto> list(Pageable pageable, ProjectSearchCondition condition) {
        
        JPQLQuery<Project> query = from(qProject);
        BooleanBuilder builder = new BooleanBuilder();

        if (condition.getSearchType() != null && !condition.getSearchType().isEmpty()
                && condition.getSearchText() != null && !condition.getSearchText().isEmpty()) {
            String keyword = condition.getSearchText();
            switch (condition.getSearchType()) {
                case "project_id":
                    builder.and(qProject.projectId.containsIgnoreCase(keyword));
                    break;
                case "project_name":
                    builder.and(qProject.projectName.containsIgnoreCase(keyword));
                    break;
                case "project_url_info":
                    builder.and(qProject.urlInfo.containsIgnoreCase(keyword));
                    break;
                default:
                    // 기본으로 아무 검색도 하지 않음
                    break;
            }
        }

        // 검색하는 데이터가 존재할 경우에
        if (builder.hasValue()) {
            query.where(builder);
        }

        JPQLQuery<ProjectResponseDto> dtoQuery = query.select(Projections.fields(
                ProjectResponseDto.class,
                qProject.projectId,
                qProject.projectName,
                qProject.urlInfo,
                qProject.customerName,
                qProject.statusCode,
                qProject.statusCode,
                qProject
        ));

        this.getQuerydsl().applyPagination(pageable, dtoQuery);

        List<ProjectResponseDto> dtoList = dtoQuery.fetch();
        long total = dtoQuery.fetchCount();

        return new PageImpl<>(dtoList, pageable, total);
    }
}