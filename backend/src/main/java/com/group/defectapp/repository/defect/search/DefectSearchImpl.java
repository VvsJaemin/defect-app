package com.group.defectapp.repository.defect.search;

import com.group.defectapp.domain.cmCode.QCommonCode;
import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.defect.DefectStatusCode;
import com.group.defectapp.domain.defect.QDefect;
import com.group.defectapp.domain.project.QProject;
import com.group.defectapp.domain.user.QUser;
import com.group.defectapp.dto.defect.DefectListDto;
import com.group.defectapp.dto.defect.DefectSearchCondition;
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

public class DefectSearchImpl extends QuerydslRepositorySupport implements DefectSearch {

    private final QDefect qDefect = QDefect.defect;
    private final QProject qProject = QProject.project;
    private final QUser qUser = QUser.user;

    private final QCommonCode statusCode = new QCommonCode("statusCode");
    private final QCommonCode seriousCode = new QCommonCode("seriousCode");
    private final QCommonCode orderCode = new QCommonCode("orderCode");
    private final QCommonCode defectDivCode = new QCommonCode("defectDivCode");

    public DefectSearchImpl() {
        super(Defect.class);
    }

    @Override
    public Page<DefectListDto> list(Pageable pageable, DefectSearchCondition condition) {
        // 공통 WHERE 조건을 미리 생성
        BooleanBuilder whereConditions = buildWhereConditions(condition);

        // 데이터 조회 쿼리 (서브쿼리 최적화: LEFT JOIN으로 변경)
        JPQLQuery<DefectListDto> dataQuery = buildDataQuery(whereConditions);

        // 정렬 적용
        applySorting(dataQuery, pageable);

        // 페이지네이션 적용
        getQuerydsl().applyPagination(pageable, dataQuery);

        // 데이터 조회
        List<DefectListDto> content = dataQuery.fetch();

        // COUNT 쿼리 최적화 (JOIN 제거하고 필요한 조건만)
        long total = buildCountQuery(whereConditions).fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanBuilder buildWhereConditions(DefectSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        if (condition.getDefectId() != null && !condition.getDefectId().isEmpty()) {
            builder.and(qDefect.defectId.containsIgnoreCase(condition.getDefectId()));
        }
        if (condition.getDefectTitle() != null && !condition.getDefectTitle().isEmpty()) {
            builder.and(qDefect.defectTitle.containsIgnoreCase(condition.getDefectTitle()));
        }
        if(condition.getStatusCode() != null && !condition.getStatusCode().isEmpty()) {
            builder.and(qDefect.statusCode.eq(condition.getStatusCode()));
        }
        if(condition.getProjectId() != null && !condition.getProjectId().isEmpty()) {
            builder.and(qDefect.projectId.eq(condition.getProjectId()));
        }
        if (condition.getAssigneeId() != null && !condition.getAssigneeId().isEmpty()) {
            builder.and(qDefect.assignee.eq(condition.getAssigneeId()));
        }

        builder.and(qDefect.openYn.eq("Y"));

        // 페이지 타입에 따른 상태 조건 추가
        if (condition.getType() != null && !condition.getType().isEmpty()) {
            switch (condition.getType()) {
                case "assigned":
                    // assigneeId 조건은 위에서 이미 처리됨
                    break;
                case "in-progress":
                    builder.and(qDefect.statusCode.in(
                            DefectStatusCode.REGISTERED.getCode(),
                            DefectStatusCode.TODO_WAITING.getCode(),
                            DefectStatusCode.ASSIGNED.getCode(),
                            DefectStatusCode.COMPLETED.getCode(),
                            DefectStatusCode.HOLD_NOT_DEFECT.getCode(),
                            DefectStatusCode.REJECTED_NOT_FIXED.getCode(),
                            DefectStatusCode.REOCCURRED.getCode(),
                            DefectStatusCode.TODO_PROCESSED.getCode()
                    ));
                    break;
                case "completed":
                    builder.and(qDefect.statusCode.in(
                            DefectStatusCode.CLOSED.getCode(),
                            DefectStatusCode.CANCELED.getCode()
                    ));
                    break;
                case "todo":
                    builder.and(qDefect.statusCode.in(
                            DefectStatusCode.TODO_PROCESSED.getCode(),
                            DefectStatusCode.TODO_WAITING.getCode()
                    ));
                    break;
            }
        }

        return builder;
    }

    /**
     * 데이터 조회 쿼리
     */
    private JPQLQuery<DefectListDto> buildDataQuery(BooleanBuilder whereConditions) {
        return from(qDefect)
                .leftJoin(qProject).on(qDefect.projectId.eq(qProject.projectId))
                .leftJoin(qUser).on(qDefect.assignee.eq(qUser.userId))
                .leftJoin(statusCode).on(statusCode.seCode.eq(qDefect.statusCode).and(statusCode.upperCode.eq("DEFECT_STATUS")))
                .leftJoin(seriousCode).on(seriousCode.seCode.eq(qDefect.seriousCode).and(seriousCode.upperCode.eq("DEFECT_SERIOUS")))
                .leftJoin(orderCode).on(orderCode.seCode.eq(qDefect.orderCode).and(orderCode.upperCode.eq("DEFECT_ORDER")))
                .leftJoin(defectDivCode).on(defectDivCode.seCode.eq(qDefect.defectDivCode).and(defectDivCode.upperCode.eq("DEFECT_DIV")))
                .where(whereConditions)
                .select(Projections.fields(
                        DefectListDto.class,
                        qDefect.defectId,
                        qProject.projectName,
                        qDefect.defectTitle,
                        qDefect.defectMenuTitle,
                        defectDivCode.codeName.as("defectDivCode"),
                        qUser.userName.as("assigneeId"),
                        statusCode.codeName.as("statusCode"),
                        seriousCode.codeName.as("seriousCode"),
                        orderCode.codeName.as("orderCode")
                ));
    }

    /**
     * COUNT 쿼리 최적화 (불필요한 JOIN 제거)
     */
    private JPQLQuery<Defect> buildCountQuery(BooleanBuilder whereConditions) {
        return from(qDefect)
                .where(whereConditions);
    }

    /**
     * 정렬 로직 분리
     */
    private void applySorting(JPQLQuery<DefectListDto> query, Pageable pageable) {
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

            switch (sortKey) {
                case "defectId":
                    query.orderBy(new OrderSpecifier<>(order, qDefect.defectId));
                    break;
                case "projectName":
                    query.orderBy(new OrderSpecifier<>(order, qProject.projectName));
                    break;
                case "defectTitle":
                    query.orderBy(new OrderSpecifier<>(order, qDefect.defectTitle));
                    break;
                case "defectMenuTitle":
                    query.orderBy(new OrderSpecifier<>(order, qDefect.defectMenuTitle));
                    break;
                case "defectDivCode":
                    query.orderBy(new OrderSpecifier<>(order, defectDivCode.codeName));  // ✅ JOIN된 코드명으로 정렬
                    break;
                case "assigneeId":
                    query.orderBy(new OrderSpecifier<>(order, qUser.userName));
                    break;
                case "statusCode":
                    query.orderBy(new OrderSpecifier<>(order, statusCode.codeName));     // ✅ JOIN된 코드명으로 정렬
                    break;
                case "seriousCode":
                    query.orderBy(new OrderSpecifier<>(order, seriousCode.codeName));    // ✅ JOIN된 코드명으로 정렬
                    break;
                case "orderCode":
                    query.orderBy(new OrderSpecifier<>(order, orderCode.codeName));      // ✅ JOIN된 코드명으로 정렬
                    break;
                default:
                    query.orderBy(new OrderSpecifier<>(Order.DESC, qDefect.defectId));
            }
        } else {
            query.orderBy(new OrderSpecifier<>(Order.DESC, qDefect.defectId));
        }
    }
}