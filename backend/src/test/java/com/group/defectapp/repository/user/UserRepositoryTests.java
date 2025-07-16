package com.group.defectapp.repository.user;

import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.user.UserListDto;
import com.group.defectapp.dto.user.UserSearchCondition;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryTests {

    // 테스트 상수 정의 - 클래스 상수로 직접 노출하여 가독성 향상
    private static final String TEST_PASSWORD = "woals1212!";
    private static final String DEVELOPER_EMAIL = "jm0820@test.co.kr";
    private static final String QA_EMAIL = "qa1@test.co.kr";
    private static final String CUSTOMER_EMAIL = "CU1@test.co.kr";
    private static final String MANAGER_EMAIL = "DM1@test.co.kr";
    private static final String SEARCH_NAME = "시니어";

    // 사용자 유형 코드
    private static final String TYPE_CUSTOMER = "CU";
    private static final String TYPE_MANAGER = "DM";
    private static final String TYPE_DEVELOPER = "DP";
    private static final String TYPE_QA = "QA";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String encodedPassword;

    @BeforeAll
    void setUp() {
        // 모든 테스트에서 사용할 암호화된 비밀번호 준비
        encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
    }

    @Test
    @DisplayName("권한별 사용자를 일괄 등록할 수 있어야 한다")
    @Transactional
    void saveMultipleUsers_ShouldPersistAllUsers() {
        // Given: 다양한 권한을 가진 사용자 리스트 생성
        List<User> users = createTestUserList();

        // When: 사용자 일괄 저장
        List<User> savedUsers = userRepository.saveAll(users);

        // Then: 저장된 사용자 수 확인
        assertEquals(users.size(), savedUsers.size(), "저장된 사용자 수가 일치하지 않습니다.");

        // 저장된 모든 사용자가 ID를 가지고 있는지 확인
        savedUsers.forEach(user ->
                assertNotNull(user.getUserId(), "저장된 사용자의 ID가 null이 아니어야 합니다."));
    }

    @Test
    @DisplayName("이름으로 사용자를 검색할 수 있어야 한다")
    void searchUsersByName_ShouldReturnMatchingUsers() {
        // Given: 검색 조건 및 페이징 설정
        Pageable pageable = PageRequest.of(0, 100, Sort.by("createdAt").descending());
        UserSearchCondition condition = UserSearchCondition.builder()
                .userId("")
                .userName(SEARCH_NAME)
                .build();

        // When: 사용자 검색
        Page<UserListDto> result = userRepository.list(condition, pageable);

        // Then: 검색 결과 확인
        assertTrue(result.getTotalElements() > 0, "검색 결과가 존재해야 합니다.");

        // 모든 결과가 검색어를 포함하는지 확인
        result.forEach(dto ->
                assertTrue(dto.getUserName().contains(SEARCH_NAME),
                        String.format("사용자 이름 '%s'에 '%s'가 포함되어 있어야 합니다.",
                                dto.getUserName(), SEARCH_NAME)));
    }

    @Test
    @DisplayName("사용자 이름을 변경할 수 있어야 한다")
    @Transactional
    void updateUserName_ShouldChangeUserNameSuccessfully() {
        // Given: 테스트 사용자 생성 및 저장
        String initialName = "개발자";
        String newUserName = "시니어 개발자";

        User testUser = createTestUser(DEVELOPER_EMAIL, encodedPassword, initialName, TYPE_DEVELOPER);
        userRepository.save(testUser);

        User savedUser = userRepository.findByUserId(DEVELOPER_EMAIL)
                .orElseThrow(() -> new AssertionError("테스트 사용자가 저장되지 않았습니다."));

        // When: 사용자 이름 변경
        savedUser.changeUserName(newUserName);
        User updatedUser = userRepository.save(savedUser);

        // Then: 변경된 이름 확인
        assertEquals(newUserName, updatedUser.getUserName(),
                "사용자 이름이 올바르게 변경되어야 합니다.");

        // DB에서 다시 조회하여 영구적으로 변경되었는지 확인
        User retrievedUser = userRepository.findByUserId(DEVELOPER_EMAIL)
                .orElseThrow(() -> new AssertionError("사용자를 찾을 수 없습니다."));
        assertEquals(newUserName, retrievedUser.getUserName(),
                "DB에 저장된 사용자 이름이 변경되어야 합니다.");
    }

    @Test
    @DisplayName("사용자를 삭제할 수 있어야 한다")
    @Transactional
    void deleteUser_ShouldRemoveUserFromDatabase() {
        // Given: 삭제할 테스트 사용자 생성 및 저장
        User testUser = createTestUser(QA_EMAIL, encodedPassword, "QA 담당자", TYPE_QA);
        userRepository.save(testUser);

        // 저장 확인
        assertTrue(userRepository.findByUserId(QA_EMAIL).isPresent(),
                "테스트용 사용자가 저장되어야 합니다.");

        // When: 사용자 삭제
        User userToDelete = userRepository.findByUserId(QA_EMAIL)
                .orElseThrow(() -> new AssertionError("삭제할 사용자를 찾을 수 없습니다."));
        userRepository.delete(userToDelete);

        // Then: 삭제 확인
        assertFalse(userRepository.findByUserId(QA_EMAIL).isPresent(),
                "사용자가 DB에서 삭제되어야 합니다.");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 시 빈 Optional이 반환되어야 한다")
    void findNonExistentUser_ShouldReturnEmptyOptional() {
        // Given: 존재하지 않는 사용자 ID
        String nonExistentUserId = "nonexistent@example.com";

        // When: 사용자 조회
        Optional<User> result = userRepository.findByUserId(nonExistentUserId);

        // Then: 빈 Optional 확인
        assertFalse(result.isPresent(), "존재하지 않는 사용자 ID로 조회 시 빈 Optional이 반환되어야 합니다.");
    }

    // 헬퍼 메소드: 테스트 사용자 리스트 생성
    private List<User> createTestUserList() {
        List<User> users = new ArrayList<>();
        users.add(createTestUser(CUSTOMER_EMAIL, encodedPassword, "고객사 담당자1", TYPE_CUSTOMER));
        users.add(createTestUser(MANAGER_EMAIL, encodedPassword, "개발사 관리자1", TYPE_MANAGER));
        users.add(createTestUser(DEVELOPER_EMAIL, encodedPassword, "개발자", TYPE_DEVELOPER));
        users.add(createTestUser(QA_EMAIL, encodedPassword, "QA 담당자1", TYPE_QA));
        return users;
    }

    // 헬퍼 메소드: 테스트 사용자 생성
    private User createTestUser(String userId, String password, String userName, String userTypeCode) {
        return User.builder()
                .userId(userId)
                .password(password)
                .userName(userName)
                .userSeCd(userTypeCode)
                .build();
    }
}