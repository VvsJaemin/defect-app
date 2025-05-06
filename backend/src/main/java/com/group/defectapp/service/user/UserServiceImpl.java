package com.group.defectapp.service.user;

import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.user.UserListDto;
import com.group.defectapp.dto.user.UserRequestDto;
import com.group.defectapp.dto.user.UserSearchCondition;
import com.group.defectapp.exception.user.UserCode;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 유효한 userSeCd 값
    private static final Set<String> VALID_USER_SE_CODES = new HashSet<>(
            Arrays.asList("CU", "DM", "DP", "MG", "QA")
    );

    @Transactional
    public void save(UserRequestDto userRequestDto) {
        User user = User.builder()
                .userId(userRequestDto.getUserId())
                .password(passwordEncoder.encode(userRequestDto.getPassword()))
                .userName(userRequestDto.getUserName())
                .userSeCd(userRequestDto.getUserSeCd())
                .build();

        userRepository.save(user);
    }

    public Page<UserListDto> getUsersList(UserSearchCondition condition, PageRequestDto pageRequestDto) {
        Pageable pageable = pageRequestDto.getPageable(Sort.by("createdAt").descending());
        return userRepository.list(condition, pageable);

    }

    @Transactional
    public void updateUser(UserRequestDto userRequestDto) {

        User user = userRepository.findByUserId(userRequestDto.getUserId())
                .orElseThrow(UserCode.USER_NOT_FOUND::getUserException);

        user.changeUserName(userRequestDto.getUserName());
        user.changePassword(passwordEncoder.encode(userRequestDto.getPassword()));
        user.changeUserSeCd(userRequestDto.getUserSeCd());

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

    public UserDetails createUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getUserId(),
                user.getPassword(),
                user.getRoles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList())
        );
    }
}