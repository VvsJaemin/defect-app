package com.group.defectapp.repository.user.search;

import com.group.defectapp.domain.cmCode.QCommonCode;
import com.group.defectapp.domain.user.QUser;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.user.UserListDto;
import com.group.defectapp.dto.user.UserResponseDto;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathBuilder;
import com.group.defectapp.dto.user.UserSearchCondition;
import org.springframework.data.domain.Sort;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class UserSearchImpl extends QuerydslRepositorySupport implements UserSearch {

    private final QUser qUser = QUser.user;
    private final QCommonCode qCommonCode = QCommonCode.commonCode;


    public UserSearchImpl() {
        super(User.class);
    }

    @Override
    public Page<UserListDto> list(UserSearchCondition condition, Pageable pageable) {
        JPQLQuery<User> query = from(qUser);

        BooleanBuilder builder = new BooleanBuilder();

        if (condition != null) {
            if (condition.getUserId() != null && !condition.getUserId().isEmpty()) {
                builder.and(qUser.userId.containsIgnoreCase(condition.getUserId()));
            }

            if (condition.getUserName() != null && !condition.getUserName().isEmpty()) {
                builder.and(qUser.userName.containsIgnoreCase(condition.getUserName()));
            }

            if (condition.getUserSeCd() != null && !condition.getUserSeCd().isEmpty()) {
                builder.and(qUser.userSeCd.eq(condition.getUserSeCd()));
            }
        }

        query.where(builder);

        JPQLQuery<UserListDto> dtoQuery = query
                .leftJoin(qCommonCode).on(qCommonCode.seCode.eq(qUser.userSeCd))
                .select(Projections.fields(UserListDto.class,
                        qUser.userId,
                        qUser.userName,
                        qCommonCode.codeName.as("userSeCd"),
                        qUser.lastLoginAt,
                        qUser.createdAt
                ));

        String sortKey = null;
        String sortOrder = null;

        if (pageable != null && pageable.getSort() != null) {
            for (Sort.Order order : pageable.getSort()) {
                sortKey = order.getProperty();
                sortOrder = order.getDirection().name();
                break; // 첫 번째 정렬 조건만 사용
            }
        }


        if (sortKey != null && !sortKey.isBlank()) {
            Order order = "desc".equalsIgnoreCase(sortOrder) ? Order.DESC : Order.ASC;

            if ("userId".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qUser.userId));
            } else if ("userName".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qUser.userName));
            } else if ("createdAt".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qUser.createdAt));
            } else if ("lastLoginAt".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qUser.lastLoginAt));
            } else if ("userSeCd".equals(sortKey)) {
                dtoQuery.orderBy(new OrderSpecifier<>(order, qCommonCode.codeName));
            }
        } else {
            dtoQuery.orderBy(new OrderSpecifier<>(Order.DESC, qUser.createdAt));
        }


        JPQLQuery<UserListDto> pageableQuery = getQuerydsl().applyPagination(pageable, dtoQuery);

        List<UserListDto> content = pageableQuery.fetch();
        long total = dtoQuery.fetchCount();

        return new PageImpl<>(content, pageable, total);
    }
}