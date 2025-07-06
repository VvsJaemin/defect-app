package com.group.defectapp.domain.user;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 1553137214L;

    public static final QUser user = new QUser("user");

    public final com.group.defectapp.domain.cmCode.QBaseEntity _super = new com.group.defectapp.domain.cmCode.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final ListPath<com.group.defectapp.domain.project.Project, com.group.defectapp.domain.project.QProject> createdProject = this.<com.group.defectapp.domain.project.Project, com.group.defectapp.domain.project.QProject>createList("createdProject", com.group.defectapp.domain.project.Project.class, com.group.defectapp.domain.project.QProject.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> lastLoginAt = createDateTime("lastLoginAt", java.time.LocalDateTime.class);

    public final StringPath password = createString("password");

    public final NumberPath<Integer> pwdFailCnt = createNumber("pwdFailCnt", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath userId = createString("userId");

    public final StringPath userName = createString("userName");

    public final StringPath userSeCd = createString("userSeCd");

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}

