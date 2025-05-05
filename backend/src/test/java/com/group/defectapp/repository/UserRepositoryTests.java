package com.group.defectapp.repository;

import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.user.UserListDto;
import com.group.defectapp.dto.user.UserSearchCondition;
import com.group.defectapp.repository.user.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserRepositoryTests {
    
    // 테스트 상수 정의
    private static class TestData {
        // 테스트 계정 정보
        static final String PASSWORD = "woals1212!";
        static final String DEVELOPER_EMAIL = "jm0820@groovysoft.co.kr";
        static final String QA_EMAIL = "qa1@test.co.kr";
        static final String CUSTOMER_EMAIL = "CU1@test.co.kr";
        static final String MANAGER_EMAIL = "DM1@test.co.kr";
        
        // 사용자 유형 코드
        static final String TYPE_CUSTOMER = "CU";
        static final String TYPE_MANAGER = "DM";
        static final String TYPE_DEVELOPER = "DP";
        static final String TYPE_QA = "QA";
    }
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Order(1)
    @Transactional
    @Commit
    public void saveMultipleUsers_ShouldPersistAllUsers() {
        // Given
        String encodedPassword = passwordEncoder.encode(TestData.PASSWORD);
        List<User> users = new ArrayList<>();
        users.add(createTestUser(TestData.CUSTOMER_EMAIL, encodedPassword, "고객사 담당자1", TestData.TYPE_CUSTOMER));
        users.add(createTestUser(TestData.MANAGER_EMAIL, encodedPassword, "개발사 관리자1", TestData.TYPE_MANAGER));
        users.add(createTestUser(TestData.DEVELOPER_EMAIL, encodedPassword, "개발자", TestData.TYPE_DEVELOPER));
        users.add(createTestUser(TestData.QA_EMAIL, encodedPassword, "QA 담당자1", TestData.TYPE_QA));
        
        // When
        List<User> savedUsers = userRepository.saveAll(users);
        
        // Then
        Assertions.assertEquals(users.size(), savedUsers.size());
    }

    @Test
    @Order(3)
    public void searchUsersByName_ShouldReturnMatchingUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 100, Sort.by("createdAt").descending());
        UserSearchCondition condition = UserSearchCondition.builder()
                .userId("")
                .userName("시니어")
                .build();
        
        // When
        Page<UserListDto> result = userRepository.list(condition, pageable);
        
        // Then
        result.getContent().forEach(System.out::println);
        // 실제 테스트에서는 여기에 구체적인 검증 로직이 필요함
    }

    @Test
    @Order(4)
    @Transactional
    @Commit
    public void updateUserName_ShouldChangeUserNameSuccessfully() {
        // Given
        String newUserName = "시니어 개발자";
        Optional<User> userOptional = userRepository.findByUserId(TestData.DEVELOPER_EMAIL);
        Assertions.assertTrue(userOptional.isPresent(), "개발자 사용자를 찾을 수 없습니다");
        User user = userOptional.get();
        
        // When
        user.changeUserName(newUserName);
        User updatedUser = userRepository.save(user);
        
        // Then
        Assertions.assertEquals(newUserName, updatedUser.getUserName(), "사용자 이름이 올바르게 변경되지 않았습니다");
    }

    @Test
    @Order(5)
    @Transactional
    @Commit
    public void deleteUser_ShouldRemoveUserFromDatabase() {
        // Given
        Optional<User> userOptional = userRepository.findByUserId(TestData.QA_EMAIL);
        Assertions.assertTrue(userOptional.isPresent(), "QA 사용자를 찾을 수 없습니다");
        User userToDelete = userOptional.get();
        
        // When
        userRepository.delete(userToDelete);
        
        // Then
        Optional<User> deletedUserOptional = userRepository.findByUserId(TestData.QA_EMAIL);
        Assertions.assertFalse(deletedUserOptional.isPresent(), "사용자가 성공적으로 삭제되지 않았습니다");
    }

    /**
     * 테스트용 사용자 객체를 생성합니다
     */
    private User createTestUser(String userId, String password, String userName, String userTypeCode) {
        return User.builder()
                .userId(userId)
                .password(password)
                .userName(userName)
                .userSeCd(userTypeCode)
                .build();
    }
}