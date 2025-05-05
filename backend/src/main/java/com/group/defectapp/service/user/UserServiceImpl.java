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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

    @Transactional
    public void updateLastLoginAt(String userId) {

      userRepository.updateLastLoginAt(userId, LocalDateTime.now());
    }

    public User findByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(UserCode.USER_NOT_FOUND::getUserException);
    }
}