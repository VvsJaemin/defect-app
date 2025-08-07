
package com.group.defectapp.repository.defectlog.search;

import com.group.defectapp.domain.cmCode.QCommonCode;
import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.defectlog.DefectLogFile;
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
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

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


        // === 메인 쿼리 실행 ===
        List<DefectLogListDto> content = queryFactory
                .from(qDefectLog)
                .leftJoin(qCommonCode).on(qCommonCode.seCode.eq(qDefectLog.statusCd))
                .leftJoin(qUser).on(qUser.userId.eq(qDefectLog.createdBy))
                .leftJoin(qDefect).on(qDefect.defectId.eq(qDefectLog.defectId))
                .leftJoin(qProject).on(qProject.projectId.eq(qDefect.projectId))
                .leftJoin(qAssignUser).on(qAssignUser.userId.eq(qDefect.assignee))
                .leftJoin(seriousCode).on(seriousCode.seCode.eq(qDefect.seriousCode).and(seriousCode.upperCode.eq("DEFECT_SERIOUS")))
                .leftJoin(orderCode).on(orderCode.seCode.eq(qDefect.orderCode).and(orderCode.upperCode.eq("DEFECT_ORDER")))
                .leftJoin(defectDivCode).on(defectDivCode.seCode.eq(qDefect.defectDivCode).and(defectDivCode.upperCode.eq("DEFECT_DIV")))

                // 결과 필드 매핑 (defectLogFiles 제외)
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

                        seriousCode.codeName.as("seriousCode"),
                        orderCode.codeName.as("orderCode"),
                        defectDivCode.codeName.as("defectDivCode")


                ))
                .where(qDefectLog.defectId.eq(defectId))
                .orderBy(qDefectLog.createdAt.desc())
                .fetch();

        // === 별도로 defectLogFiles 정보 조회하여 매핑 ===
        if (!content.isEmpty()) {
            // DefectLog에서 사용하는 logSeq 목록 추출
            List<Integer> logSeqs = content.stream()
                    .map(DefectLogListDto::getLogSeq)
                    .distinct()
                    .collect(Collectors.toList());

            // defectLogFiles 정보를 별도 쿼리로 조회
            List<DefectLog> defectLogsWithFiles = queryFactory
                    .selectFrom(qDefectLog)
                    .where(qDefectLog.logSeq.in(logSeqs))
                    .fetch();

            // logSeq별로 파일 정보를 맵으로 구성 (SortedSet으로 최대 3개)
            Map<Integer, SortedSet<DefectLogFile>> defectLogFilesMap = defectLogsWithFiles.stream()
                    .collect(Collectors.toMap(
                            DefectLog::getLogSeq,
                            defectLog -> defectLog.getDefectLogFiles().stream()
                                    .limit(3) // 최대 3개로 제한
                                    .collect(Collectors.toCollection(TreeSet::new))
                    ));

            // 각 DTO에 파일 정보 설정
            content.forEach(dto -> {
                SortedSet<DefectLogFile> files = defectLogFilesMap.get(dto.getLogSeq());
                if (files != null) {
                    dto.setDefectLogFiles(files);
                }
            });
        }

        return new PageImpl<>(content, pageable, content.size());
    }
}