package com.group.defectapp.domain.defect;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDefect is a Querydsl query type for Defect
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDefect extends EntityPathBase<Defect> {

    private static final long serialVersionUID = 2023187134L;

    public static final QDefect defect = new QDefect("defect");

    public final com.group.defectapp.domain.cmCode.QBaseEntity _super = new com.group.defectapp.domain.cmCode.QBaseEntity(this);

    public final StringPath assignee = createString("assignee");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath createdBy = createString("createdBy");

    public final StringPath defectContent = createString("defectContent");

    public final StringPath defectDivCode = createString("defectDivCode");

    public final StringPath defectEtcContent = createString("defectEtcContent");

    public final SetPath<DefectFile, QDefectFile> defectFiles = this.<DefectFile, QDefectFile>createSet("defectFiles", DefectFile.class, QDefectFile.class, PathInits.DIRECT2);

    public final StringPath defectId = createString("defectId");

    public final StringPath defectMenuTitle = createString("defectMenuTitle");

    public final StringPath defectTitle = createString("defectTitle");

    public final StringPath defectUrlInfo = createString("defectUrlInfo");

    public final StringPath openYn = createString("openYn");

    public final StringPath orderCode = createString("orderCode");

    public final StringPath projectId = createString("projectId");

    public final StringPath seriousCode = createString("seriousCode");

    public final StringPath statusCode = createString("statusCode");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath updatedBy = createString("updatedBy");

    public QDefect(String variable) {
        super(Defect.class, forVariable(variable));
    }

    public QDefect(Path<? extends Defect> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDefect(PathMetadata metadata) {
        super(Defect.class, metadata);
    }

}

