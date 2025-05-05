package com.group.defectapp.domain.defectlog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDefectLog is a Querydsl query type for DefectLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDefectLog extends EntityPathBase<DefectLog> {

    private static final long serialVersionUID = 428613128L;

    public static final QDefectLog defectLog = new QDefectLog("defectLog");

    public final com.group.defectapp.domain.cmCode.QBaseEntity _super = new com.group.defectapp.domain.cmCode.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath createdBy = createString("createdBy");

    public final StringPath defectId = createString("defectId");

    public final SetPath<DefectLogFile, QDefectLogFile> defectLogFiles = this.<DefectLogFile, QDefectLogFile>createSet("defectLogFiles", DefectLogFile.class, QDefectLogFile.class, PathInits.DIRECT2);

    public final StringPath logCt = createString("logCt");

    public final NumberPath<Integer> logSeq = createNumber("logSeq", Integer.class);

    public final StringPath logTitle = createString("logTitle");

    public final StringPath statusCd = createString("statusCd");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QDefectLog(String variable) {
        super(DefectLog.class, forVariable(variable));
    }

    public QDefectLog(Path<? extends DefectLog> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDefectLog(PathMetadata metadata) {
        super(DefectLog.class, metadata);
    }

}

