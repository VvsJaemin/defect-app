package com.group.defectapp.repository.defectlog.search;

import com.group.defectapp.domain.defectlog.DefectLog;
import com.group.defectapp.domain.defectlog.QDefectLog;
import com.group.defectapp.dto.defectlog.DefectLogListDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class DefectLogSearchImpl extends QuerydslRepositorySupport implements DefectLogSearch {

    private final QDefectLog qDefectLog = QDefectLog.defectLog;

    public DefectLogSearchImpl() {
        super(DefectLog.class);
    }


    @Override
    public Page<DefectLogListDto> list(Pageable pageable, String defectId) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(getEntityManager());

        List<DefectLogListDto> content = queryFactory
                .select(Projections.constructor(DefectLogListDto.class,
                        qDefectLog.logSeq,
                        qDefectLog.defectId,
                        qDefectLog.logTitle,
                        qDefectLog.logCt,
                        qDefectLog.statusCd,
                        qDefectLog.createdAt,
                        qDefectLog.createdBy))
                .from(qDefectLog)
                .orderBy(qDefectLog.createdBy.desc())
                .where(qDefectLog.defectId.eq(defectId))
                .fetch();

        return new PageImpl<>(content, pageable, content.size());
    }

}