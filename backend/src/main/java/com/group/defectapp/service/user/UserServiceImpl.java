package com.group.defectapp.service.user;

import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.user.UserListDto;
import com.group.defectapp.dto.user.UserRequestDto;
import com.group.defectapp.dto.user.UserResponseDto;
import com.group.defectapp.dto.user.UserSearchCondition;
import com.group.defectapp.exception.user.UserCode;
import com.group.defectapp.repository.cmCode.CommonCodeRepository;
import com.group.defectapp.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CommonCodeRepository commonCodeRepository;

    // 유효한 userSeCd 값
    private static final Set<String> VALID_USER_SE_CODES = new HashSet<>(
            Arrays.asList("CU", "DM", "DP", "MG", "QA")
    );

    @Transactional
    public void save(UserRequestDto userRequestDto) {

        // 아이디 중복 체크
        if (checkUserIdDuplicate(userRequestDto.getUserId())) {
            throw UserCode.USER_ALREADY_EXISTS.getUserException();
        }


        User user = User.builder()
                .userId(userRequestDto.getUserId())
                .password(passwordEncoder.encode(userRequestDto.getPassword()))
                .userName(userRequestDto.getUserName())
                .userSeCd(userRequestDto.getUserSeCd())
                .build();

        userRepository.save(user);
    }

    @Override
    public Page<UserListDto> getUsersList(Map<String, Object> paramMap, PageRequestDto pageRequestDto) {

        int pageIndex = Integer.parseInt(Objects.toString(paramMap.get("pageIndex")));
        int pageSize = Integer.parseInt(Objects.toString(paramMap.get("pageSize")));
        String sortKey = Objects.toString(paramMap.get("sortKey"));
        String sortOrder = Objects.toString(paramMap.get("sortOrder"));

        pageRequestDto = PageRequestDto.builder()
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .sortKey(sortKey)
                .sortOrder(sortOrder)
                .build();

        UserSearchCondition condition = UserSearchCondition.builder()
                .userId((String) paramMap.getOrDefault("userId", null))
                .userName((String) paramMap.getOrDefault("userName", null))
                .userSeCd((String) paramMap.getOrDefault("userSeCd", null))
                .build();

        Pageable pageable = pageRequestDto.getPageable();

        return userRepository.list(condition, pageable);
    }


    public UserResponseDto readUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(UserCode.USER_NOT_FOUND::getUserException);

        Optional<CommonCode> seCode = commonCodeRepository.findBySeCode(user.getUserSeCd());

        String codeName = seCode.isPresent() ? seCode.get().getCodeName() : "";

        return UserResponseDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .userSeCd(user.getUserSeCd())
                .userSeNm(codeName)
                .lastLoginAt(user.getLastLoginAt())
                .firstRegDtm(user.getCreatedAt())
                .fnlUdtDtm(user.getUpdatedAt())
                .build();

    }

    @Transactional
    public void updateUser(UserRequestDto userRequestDto) {

        User user = userRepository.findByUserId(userRequestDto.getUserId())
                .orElseThrow(UserCode.USER_NOT_FOUND::getUserException);

        user.changeUserName(userRequestDto.getUserName());
        user.changeUserSeCd(userRequestDto.getUserSeCd());

        if(Objects.nonNull(userRequestDto.getPassword())) {
            user.changePassword(passwordEncoder.encode(userRequestDto.getPassword()));
        }

    }

    @Transactional
    public void deleteUser(String username) {
        User user = findByUserId(username);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void deleteUsers(List<String> userIds) {
        userRepository.deleteAllByIdIn(userIds);
    }

    @Transactional
    public void updateLastLoginAt(String userId) {

      userRepository.updateLastLoginAt(userId, LocalDateTime.now());
    }

    public User findByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(UserCode.USER_NOT_FOUND::getUserException);
    }

    public boolean checkUserIdDuplicate(String userId) {
        return userRepository.findByUserId(userId).isPresent();
    }


}