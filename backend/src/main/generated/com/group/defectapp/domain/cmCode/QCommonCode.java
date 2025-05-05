package com.group.defectapp.domain.cmCode;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCommonCode is a Querydsl query type for CommonCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCommonCode extends EntityPathBase<CommonCode> {

    private static final long serialVersionUID = -298301057L;

    public static final QCommonCode commonCode = new QCommonCode("commonCode");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final StringPath codeName = createString("codeName");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath etcInfo = createString("etcInfo");

    public final StringPath firstRegId = createString("firstRegId");

    public final StringPath fnlUdtId = createString("fnlUdtId");

    public final StringPath seCode = createString("seCode");

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath upperCode = createString("upperCode");

    public final StringPath useYn = createString("useYn");

    public QCommonCode(String variable) {
        super(CommonCode.class, forVariable(variable));
    }

    public QCommonCode(Path<? extends CommonCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCommonCode(PathMetadata metadata) {
        super(CommonCode.class, metadata);
    }

}

