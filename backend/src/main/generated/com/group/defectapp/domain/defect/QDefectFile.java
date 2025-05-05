package com.group.defectapp.domain.defect;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDefectFile is a Querydsl query type for DefectFile
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QDefectFile extends BeanPath<DefectFile> {

    private static final long serialVersionUID = -995279526L;

    public static final QDefectFile defectFile = new QDefectFile("defectFile");

    public final StringPath file_path = createString("file_path");

    public final StringPath file_se_cd = createString("file_se_cd");

    public final DateTimePath<java.time.LocalDateTime> first_reg_dtm = createDateTime("first_reg_dtm", java.time.LocalDateTime.class);

    public final StringPath first_reg_id = createString("first_reg_id");

    public final NumberPath<Integer> idx = createNumber("idx", Integer.class);

    public final StringPath org_file_name = createString("org_file_name");

    public final StringPath sys_file_name = createString("sys_file_name");

    public QDefectFile(String variable) {
        super(DefectFile.class, forVariable(variable));
    }

    public QDefectFile(Path<? extends DefectFile> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDefectFile(PathMetadata metadata) {
        super(DefectFile.class, metadata);
    }

}

