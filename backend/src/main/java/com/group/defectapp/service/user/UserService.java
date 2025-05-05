package com.group.defectapp.service.user;

import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.user.UserListDto;
import com.group.defectapp.dto.user.UserRequestDto;
import com.group.defectapp.dto.user.UserSearchCondition;
import org.springframework.data.domain.Page;

public interface UserService {

    /**
     * 사용자 정보를 저장합니다.
     *
     * @param userRequestDto 사용자 저장 요청 DTO
     */
    void save(UserRequestDto userRequestDto);

    /**
     * 검색 조건에 따른 사용자 목록을 페이징하여 조회합니다.
     *
     * @param condition 사용자 검색 조건
     * @param pageRequestDto 페이지 요청 DTO
     * @return 사용자 목록 페이지
     */
    Page<UserListDto> getUsersList(UserSearchCondition condition, PageRequestDto pageRequestDto);

    /**
     * 사용자 정보를 수정합니다.
     *
     * @param userRequestDto 사용자 수정 요청 DTO
     */
    void updateUser(UserRequestDto userRequestDto);

    /**
     * 사용자를 삭제합니다.
     *
     * @param username 삭제할 사용자 ID
     */
    void deleteUser(String username);

    /**
     * 사용자의 마지막 로그인 시각을 업데이트합니다.
     *
     * @param userId 사용자 ID
     */
    void updateLastLoginAt(String userId);

    /**
     * 사용자 ID로 사용자를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return User 엔티티
     */
    User findByUserId(String userId);

}