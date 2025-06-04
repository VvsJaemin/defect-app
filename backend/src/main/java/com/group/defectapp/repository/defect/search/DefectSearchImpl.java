package com.group.defectapp.repository.defect.search;

import com.group.defectapp.domain.cmCode.QCommonCode;
import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.defect.QDefect;
import com.group.defectapp.domain.project.QProject;
import com.group.defectapp.domain.user.QUser;
import com.group.defectapp.dto.defect.DefectListDto;
import com.group.defectapp.dto.defect.DefectSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class DefectSearchImpl extends QuerydslRepositorySupport implements DefectSearch {

    private final QDefect qDefect = QDefect.defect;
    private final QProject qProject = QProject.project;
    private final QUser qUser = QUser.user;

    public DefectSearchImpl() {
        super(Defect.class);
    }

    @Override
    public Page<DefectListDto> list(Pageable pageable, DefectSearchCondition condition) {
        JPQLQuery<Defect> query = from(qDefect);

        // Join
        query.leftJoin(qProject).on(qDefect.projectId.eq(qProject.projectId));
        query.leftJoin(qUser).on(qDefect.assignee.eq(qUser.userId));

        // 조건 필터링
        BooleanBuilder builder = new BooleanBuilder();

        if (condition.getDefectId() != null && !condition.getDefectId().isEmpty()) {
            builder.and(qDefect.defectId.containsIgnoreCase(condition.getDefectId()));
        }
        if (condition.getDefectTitle() != null && !condition.getDefectTitle().isEmpty()) {
            builder.and(qDefect.defectTitle.containsIgnoreCase(condition.getDefectTitle()));
        }
//        if (condition.getStatusCode() != null && !condition.getStatusCode().isEmpty()) {
//            builder.and(qDefect.statusCode.eq(condition.getStatusCode()));
//        }
//        if (condition.getSearchType() != null && !condition.getSearchType().isEmpty()) {
//            String keyword = condition.getSearchText();
//            switch (condition.getSearchType()) {
//                case "defect_id":
//                    builder.and(qDefect.defectId.containsIgnoreCase(keyword));
//                    break;
//                case "defect_title":
//                    builder.and(qDefect.defectTitle.containsIgnoreCase(keyword));
//                    break;
//                case "defect_url":
//                    builder.and(qDefect.defectUrlInfo.containsIgnoreCase(keyword));
//                    break;
//            }
//        }

        if (builder.hasValue()) {
            query.where(builder);
        }

        // 공통코드 서브쿼리
        QCommonCode statusCode = new QCommonCode("statusCode");
        QCommonCode seriousCode = new QCommonCode("seriousCode");
        QCommonCode orderCode = new QCommonCode("orderCode");
        QCommonCode defectDivCode = new QCommonCode("defectDivCode");

        JPQLQuery<String> statusNameQuery = JPAExpressions
                .select(statusCode.codeName)
                .from(statusCode)
                .where(statusCode.seCode.eq(qDefect.statusCode));

        JPQLQuery<String> seriousNameQuery = JPAExpressions
                .select(seriousCode.codeName)
                .from(seriousCode)
                .where(seriousCode.seCode.eq(qDefect.seriousCode));

        JPQLQuery<String> orderNameQuery = JPAExpressions
                .select(orderCode.codeName)
                .from(orderCode)
                .where(orderCode.seCode.eq(qDefect.orderCode));

        JPQLQuery<String> defectDivNameQuery = JPAExpressions
                .select(defectDivCode.codeName)
                .from(defectDivCode)
                .where(defectDivCode.seCode.eq(qDefect.defectDivCode));

        JPQLQuery<DefectListDto> dtoQuery = query.select(
                Projections.fields(
                        DefectListDto.class,
                        qDefect.defectId,
                        qProject.projectName,
                        qDefect.defectTitle,
                        qDefect.defectMenuTitle,
                        ExpressionUtils.as(defectDivNameQuery, "defectDivCode"),
                        qUser.userName.as("assigneeId"),
                        ExpressionUtils.as(statusNameQuery, "statusCode"),
                        ExpressionUtils.as(seriousNameQuery, "seriousCode"),
                        ExpressionUtils.as(orderNameQuery, "orderCode")
                )
        );

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
            if ("defectId".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qDefect.defectId));
            } else if ("projectName".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qProject.projectName));
            } else if ("defectTitle".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qDefect.defectTitle));
            } else if ("defectMenuTitle".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qDefect.defectMenuTitle));
            } else if ("defectDivCode".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qDefect.defectDivCode));
            } else if ("assigneeId".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qUser.userName));
            } else if ("statusCode".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, statusNameQuery));
            } else if ("seriousCode".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, seriousNameQuery));
            } else if ("orderCode".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, orderNameQuery));
            } else {
                dtoQuery.orderBy(new OrderSpecifier<>(Order.DESC, qProject.createdAt));
            }
        } else {
            dtoQuery.orderBy(new OrderSpecifier<>(Order.DESC, qProject.createdAt));
        }


        getQuerydsl().applyPagination(pageable, dtoQuery);

        List<DefectListDto> dtoList = dtoQuery.fetch();
        long total = dtoQuery.fetchCount(); // 또는 dtoQuery.fetchResults().getTotal();

        return new PageImpl<>(dtoList, pageable, total);
    }
}
