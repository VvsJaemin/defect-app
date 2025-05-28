package com.group.defectapp.repository.project.search;

import com.group.defectapp.domain.cmCode.QCommonCode;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.domain.project.QProject;
import com.group.defectapp.dto.project.ProjectResponseDto;
import com.group.defectapp.dto.project.ProjectSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class ProjectSearchImpl extends QuerydslRepositorySupport implements ProjectSearch {

    private final QProject qProject = QProject.project;
    private final QCommonCode qCommonCode = QCommonCode.commonCode; // tb_cm_code를 매핑하는 QCode

    public ProjectSearchImpl() {
        super(Project.class);
    }
    
    @Override
    public Page<ProjectResponseDto> list(Pageable pageable, ProjectSearchCondition condition) {
        
        JPQLQuery<Project> query = from(qProject);
        BooleanBuilder builder = new BooleanBuilder();


        if (condition != null) {
            if (condition.getProjectName() != null && !condition.getProjectName().isEmpty()) {
                builder.and(qProject.projectName.containsIgnoreCase(condition.getProjectName()));
            }

            if (condition.getUrlInfo() != null && !condition.getUrlInfo().isEmpty()) {
                builder.and(qProject.urlInfo.containsIgnoreCase(condition.getUrlInfo()));
            }

            if (condition.getProjectState() != null && !condition.getProjectState().isEmpty()) {
                builder.and(qProject.statusCode.eq(condition.getProjectState()));
            }

            if (condition.getCustomerName() != null && !condition.getCustomerName().isEmpty()) {
                builder.and(qProject.customerName.containsIgnoreCase(condition.getCustomerName()));
            }

        }




        // 검색하는 데이터가 존재할 경우에
        if (builder.hasValue()) {
            query.where(builder);
        }

        JPQLQuery<ProjectResponseDto> dtoQuery = query
                .leftJoin(qCommonCode).on(qCommonCode.seCode.eq(qProject.statusCode))
                .select(Projections.fields(
                        ProjectResponseDto.class,
                        qProject.projectId,
                        qProject.projectName,
                        qProject.urlInfo,
                        qProject.customerName,
                        qProject.createdAt,
                        qProject.projAssignedUsers.size().as("assignedUserCnt"),
                        qCommonCode.codeName.as("statusCode")
                ));

        String sortKey = null;
        String sortOrder = null;

        if (pageable != null && pageable.getSort() != null) {
            for (Sort.Order order : pageable.getSort()) {
                sortKey = order.getProperty();
                sortOrder = order.getDirection().name();
                break; // 첫 번째 정렬 조건만 사용
            }
        }

        if (sortKey != null && !sortKey.isBlank()) {
            Order order = "desc".equalsIgnoreCase(sortOrder) ? Order.DESC : Order.ASC;

            // 각 필드에 대한 정렬 처리
            if ("projectId".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qProject.projectId));
            } else if ("projectName".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qProject.projectName));
            } else if ("createdAt".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qProject.createdAt));
            } else if ("urlInfo".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qProject.urlInfo));
            } else if ("customerName".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qProject.customerName));
            } else if ("assignedUserCnt".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qProject.projAssignedUsers.size()));
            } else if ("statusCode".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qCommonCode.codeName));
            }
        } else {
            dtoQuery.orderBy(new OrderSpecifier<>(Order.DESC, qProject.createdAt));
        }


        this.getQuerydsl().applyPagination(pageable, dtoQuery);

        List<ProjectResponseDto> dtoList = dtoQuery.fetch();
        long total = dtoQuery.fetchCount();

        return new PageImpl<>(dtoList, pageable, total);
    }
}