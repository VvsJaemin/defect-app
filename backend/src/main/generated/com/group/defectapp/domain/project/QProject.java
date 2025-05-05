package com.group.defectapp.domain.project;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProject is a Querydsl query type for Project
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProject extends EntityPathBase<Project> {

    private static final long serialVersionUID = 878048052L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProject project = new QProject("project");

    public final com.group.defectapp.domain.cmCode.QBaseEntity _super = new com.group.defectapp.domain.cmCode.QBaseEntity(this);

    public final com.group.defectapp.domain.user.QUser assignee;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath createdBy = createString("createdBy");

    public final StringPath customerName = createString("customerName");

    public final StringPath etcInfo = createString("etcInfo");

    public final SetPath<String, StringPath> projAssignedUsers = this.<String, StringPath>createSet("projAssignedUsers", String.class, StringPath.class, PathInits.DIRECT2);

    public final StringPath projectId = createString("projectId");

    public final StringPath projectName = createString("projectName");

    public final StringPath statusCode = createString("statusCode");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath updatedBy = createString("updatedBy");

    public final StringPath urlInfo = createString("urlInfo");

    public final StringPath useYn = createString("useYn");

    public QProject(String variable) {
        this(Project.class, forVariable(variable), INITS);
    }

    public QProject(Path<? extends Project> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProject(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProject(PathMetadata metadata, PathInits inits) {
        this(Project.class, metadata, inits);
    }

    public QProject(Class<? extends Project> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.assignee = inits.isInitialized("assignee") ? new com.group.defectapp.domain.user.QUser(forProperty("assignee")) : null;
    }

}

