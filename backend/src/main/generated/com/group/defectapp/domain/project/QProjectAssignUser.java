package com.group.defectapp.domain.project;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QProjectAssignUser is a Querydsl query type for ProjectAssignUser
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QProjectAssignUser extends BeanPath<ProjectAssignUser> {

    private static final long serialVersionUID = -1775685714L;

    public static final QProjectAssignUser projectAssignUser = new QProjectAssignUser("projectAssignUser");

    public final StringPath projectId = createString("projectId");

    public final StringPath userId = createString("userId");

    public QProjectAssignUser(String variable) {
        super(ProjectAssignUser.class, forVariable(variable));
    }

    public QProjectAssignUser(Path<? extends ProjectAssignUser> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProjectAssignUser(PathMetadata metadata) {
        super(ProjectAssignUser.class, metadata);
    }

}

