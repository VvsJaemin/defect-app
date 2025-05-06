package com.group.defectapp.repository.user;

import com.group.defectapp.domain.user.User;
import com.group.defectapp.repository.user.search.UserSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserSearch {

    Optional<User> findByUserId(String userId);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt WHERE u.userId = :userId")
    void updateLastLoginAt(@Param("userId") String userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    @Modifying
    @Query("DELETE FROM User u WHERE u.userId in :userIds ")
    void deleteAllByIdIn(@Param("userIds") List<String> userIds);
}