package com.group.defectapp.domain.defectlog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDefectLogFile is a Querydsl query type for DefectLogFile
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QDefectLogFile extends BeanPath<DefectLogFile> {

    private static final long serialVersionUID = 450839460L;

    public static final QDefectLogFile defectLogFile = new QDefectLogFile("defectLogFile");

    public final StringPath defectId = createString("defectId");

    public final StringPath file_path = createString("file_path");

    public final StringPath file_se_cd = createString("file_se_cd");

    public final DateTimePath<java.time.LocalDateTime> first_reg_dtm = createDateTime("first_reg_dtm", java.time.LocalDateTime.class);

    public final StringPath first_reg_id = createString("first_reg_id");

    public final NumberPath<Integer> idx = createNumber("idx", Integer.class);

    public final StringPath org_file_name = createString("org_file_name");

    public final StringPath sys_file_name = createString("sys_file_name");

    public QDefectLogFile(String variable) {
        super(DefectLogFile.class, forVariable(variable));
    }

    public QDefectLogFile(Path<? extends DefectLogFile> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDefectLogFile(PathMetadata metadata) {
        super(DefectLogFile.class, metadata);
    }

}

