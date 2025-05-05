package com.group.defectapp.repository.user.search;

import com.group.defectapp.domain.user.QUser;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.user.UserListDto;
import com.group.defectapp.dto.user.UserResponseDto;
import com.group.defectapp.dto.user.UserSearchCondition;
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

    public UserSearchImpl() {
        super(User.class);
    }
    @Override
    public Page<UserListDto> list(UserSearchCondition condition, Pageable pageable) {
        JPQLQuery<User> query = from(qUser);

        BooleanBuilder builder = new BooleanBuilder();
        
        if(condition != null) {
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

        JPQLQuery<UserListDto> dtoQuery = query.select(Projections.fields(UserListDto.class,
                qUser.userId,
                qUser.userName,
                qUser.userSeCd,
                qUser.lastLoginAt,
                qUser.createdAt.as("first_reg_dtm")));

        // 정렬 적용
        JPQLQuery<UserListDto> pageableQuery = getQuerydsl().applyPagination(pageable, dtoQuery);

        // 결과 조회
        List<UserListDto> content = pageableQuery.fetch();

        // 전체 카운트 조회
        long total = dtoQuery.fetchCount();

        // Page 객체 생성하여 반환
        return new PageImpl<>(content, pageable, total);
    }
}