
package com.group.defectapp.repository.defectlog.search;

import com.group.defectapp.domain.cmCode.QCommonCode;
import com.group.defectapp.domain.defect.QDefect;
import com.group.defectapp.domain.defectlog.DefectLog;
import com.group.defectapp.domain.defectlog.QDefectLog;
import com.group.defectapp.domain.project.QProject;
import com.group.defectapp.domain.user.QUser;
import com.group.defectapp.dto.defectlog.DefectLogListDto;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class DefectLogSearchImpl extends QuerydslRepositorySupport implements DefectLogSearch {

    private final QDefectLog qDefectLog = QDefectLog.defectLog;
    private final QUser qUser = QUser.user;
    private final QCommonCode qCommonCode = QCommonCode.commonCode;
    private final QDefect qDefect = QDefect.defect;
    private final QProject qProject = QProject.project;

    public DefectLogSearchImpl() {
        super(DefectLog.class);
    }

    @Override
    public Page<DefectLogListDto> list(Pageable pageable, String defectId) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(getEntityManager());

        // === 별칭 엔티티 생성 ===
        QUser qAssignUser = new QUser("assignUser");
        QCommonCode seriousCode = new QCommonCode("seriousCode");
        QCommonCode orderCode = new QCommonCode("orderCode");
        QCommonCode defectDivCode = new QCommonCode("defectDivCode");

        // === 코드명 조회 서브쿼리 ===
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

        // === 메인 쿼리 실행 ===
        List<DefectLogListDto> content = queryFactory
                .from(qDefectLog)
                .leftJoin(qCommonCode).on(qCommonCode.seCode.eq(qDefectLog.statusCd))
                .leftJoin(qUser).on(qUser.userId.eq(qDefectLog.createdBy))
                .leftJoin(qDefect).on(qDefect.defectId.eq(qDefectLog.defectId))
                .leftJoin(qProject).on(qProject.projectId.eq(qDefect.projectId))
                .leftJoin(qAssignUser).on(qAssignUser.userId.eq(qDefect.assignee))
                // 결과 필드 매핑
                .select(Projections.fields(
                        DefectLogListDto.class,

                        qDefectLog.logSeq,
                        qDefectLog.defectId,
                        qDefectLog.logTitle,
                        qDefectLog.logCt,
                        qDefectLog.statusCd,
                        qDefectLog.createdAt,

                        qDefect.defectTitle.as("defectTitle"),
                        qDefect.defectUrlInfo.as("defectUrlInfo"),
                        qDefect.defectMenuTitle.as("defectMenuTitle"),
                        qDefect.assignee.as("assignUserId"),

                        qAssignUser.userName.as("assignUserName"),
                        qCommonCode.codeName.as("statusCode"),
                        qUser.userName.as("createdBy"),

                        qProject.customerName.as("customerName"),

                        ExpressionUtils.as(seriousNameQuery, "seriousCode"),
                        ExpressionUtils.as(orderNameQuery, "orderCode"),
                        ExpressionUtils.as(defectDivNameQuery, "defectDivCode")
                ))
                .where(qDefectLog.defectId.eq(defectId))
                .orderBy(qDefectLog.createdAt.desc())
                .fetch();

        return new PageImpl<>(content, pageable, content.size());
    }
}