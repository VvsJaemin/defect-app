package com.group.defectapp.repository.defect.search;

import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.defect.QDefect;
import com.group.defectapp.domain.user.QUser;
import com.group.defectapp.dto.defect.DefectListDto;
import com.group.defectapp.dto.defect.DefectSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class DefectSearchImpl extends QuerydslRepositorySupport implements DefectSearch {

    // Q 클래스 상수로 정의
    private final QDefect qDefect = QDefect.defect;
    private final QUser qUser = QUser.user;

    public DefectSearchImpl() {
        super(Defect.class);
    }

    @Override
    public Page<DefectListDto> list(Pageable pageable, DefectSearchCondition condition) {
        // 기본 쿼리 생성 및 조인
        JPQLQuery<Defect> query = from(qDefect);

        BooleanBuilder builder = new BooleanBuilder();


        if (condition.getProjectId() != null && !condition.getProjectId().isEmpty()) {
            builder.and(qDefect.projectId.eq(condition.getProjectId()));
        }

        if (condition.getAssigneeId() != null && !condition.getAssigneeId().isEmpty()) {
            builder.and(qUser.userId.eq(condition.getAssigneeId()));
        }

        if (condition.getStatusCode() != null && !condition.getStatusCode().isEmpty()) {
            builder.and(qDefect.statusCode.eq(condition.getStatusCode()));
        }

        if (condition.getSearchType() != null && !condition.getSearchType().isEmpty()) {
            String keyword = condition.getSearchText();
            switch (condition.getSearchType()) {
                case "defect_id":
                    builder.and(qDefect.defectId.containsIgnoreCase(keyword));
                    break;
                case "defect_title":
                    builder.and(qDefect.defectTitle.containsIgnoreCase(keyword));
                    break;
                case "defect_url":
                    builder.and(qDefect.defectUrlInfo.containsIgnoreCase(keyword));
                    break;
                default:
                    break;
            }
        }

        // 검색하는 데이터가 존재할 경우에
        if (builder.hasValue()) {
            query.where(builder);
        }


        // DTO 매핑 쿼리 생성
        JPQLQuery<DefectListDto> dtoQuery = createDtoQuery(query);

        // 페이지네이션 적용
        this.getQuerydsl().applyPagination(pageable, dtoQuery);

        // 결과 조회 및 페이지 객체 반환
        List<DefectListDto> dtoList = dtoQuery.fetch();
        long total = dtoQuery.fetchCount();

        return new PageImpl<>(dtoList, pageable, total);
    }


    // DTO 프로젝션 쿼리 생성
    private JPQLQuery<DefectListDto> createDtoQuery(JPQLQuery<Defect> query) {
        return query.select(
                Projections.fields(
                        DefectListDto.class,
                        qDefect.defectId,
                        qDefect.projectId,
                        qDefect.statusCode,
                        qDefect.seriousCode,
                        qDefect.orderCode,
                        qDefect.defectDivCode,
                        qDefect.defectTitle,
                        qDefect.defectMenuTitle,
                        qDefect.defectUrlInfo,
                        qDefect.openYn,
                        qDefect.assignee.as("assign_user_id"),
                        qDefect.createdAt,
                        qDefect.createdBy,
                        qDefect.updatedAt,
                        qDefect.updatedBy
//                        qDefect.images.size().as("imageCount")
                )
        ).groupBy(qDefect.defectId);
    }
}