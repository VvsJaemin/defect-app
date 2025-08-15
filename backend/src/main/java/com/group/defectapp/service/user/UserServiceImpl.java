
package com.group.defectapp.service.user;

import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.common.PageRequestDto;
import com.group.defectapp.dto.user.UserListDto;
import com.group.defectapp.dto.user.UserRequestDto;
import com.group.defectapp.dto.user.UserResponseDto;
import com.group.defectapp.dto.user.UserSearchCondition;
import com.group.defectapp.exception.user.UserCode;
import com.group.defectapp.repository.cmCode.CommonCodeRepository;
import com.group.defectapp.repository.defect.DefectRepository;
import com.group.defectapp.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CommonCodeRepository commonCodeRepository;
    private final DefectRepository defectRepository;
    private final JavaMailSender javaMailSender;

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
    public String forgetPassword(String userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(UserCode.USER_NOT_FOUND::getUserException);

        // 임시 비밀번호 생성
        String tempPassword = generateTempPassword();

        // 사용자 비밀번호를 임시 비밀번호로 변경
        user.changePassword(passwordEncoder.encode(tempPassword));

        // 비밀번호 실패 횟수 초기화
        resetPwdFailCnt(userId);

        // 임시 비밀번호를 이메일로 전송
        sendTempPasswordEmail(user, tempPassword);

        return "임시 비밀번호가 이메일로 전송되었습니다.";

    }

    @Transactional
    public void updateUser(UserRequestDto userRequestDto) {

        User user = userRepository.findByUserId(userRequestDto.getUserId())
                .orElseThrow(UserCode.USER_NOT_FOUND::getUserException);

        user.changeUserName(userRequestDto.getUserName());
        user.changeUserSeCd(userRequestDto.getUserSeCd());

        if(ObjectUtils.isNotEmpty(userRequestDto.getPassword())) {
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

        for (String userId : userIds) {
            // 1. 해당 사용자가 담당자로 할당된 결함 개수 확인
            long assignedDefectCount = defectRepository.countByAssignee(userId);

            if (assignedDefectCount > 0) {
                // 2. 담당자 할당 해제 (assignee를 NULL로 변경)
                defectRepository.updateAssigneeToNull(userId);
            }
        }

        // 3. 사용자 삭제
        userRepository.deleteAllByIdIn(userIds);
    }

    @Transactional
    public void updateLastLoginAt(String userId) {

        userRepository.updateLastLoginAt(userId, LocalDateTime.now());
    }

    @Transactional
    public int updatePwdFailCnt(String userId) {
        int pwdFailedCnt = userRepository.updatePwnFailedCnt(userId);
        return pwdFailedCnt;
    }

    @Transactional
    public void resetPwdFailCnt(String userId) {
        userRepository.resetPwdFailCnt(userId);
    }

    /**
     * 임시 비밀번호 생성
     * 영문 대소문자, 숫자를 포함한 8자리 랜덤 문자열 생성
     */
    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder tempPassword = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            tempPassword.append(chars.charAt(random.nextInt(chars.length())));
        }

        return tempPassword.toString();
    }

    /**
     * 임시 비밀번호를 이메일로 전송
     * @param user 사용자 정보
     * @param tempPassword 임시 비밀번호
     */
    private void sendTempPasswordEmail(User user, String tempPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            // 수신자 설정 - User 엔티티에 이메일 필드가 있다면 user.getEmail() 사용
            // 현재는 userId를 이메일로 가정
            String userEmail = user.getUserId(); // 또는 user.getEmail()이 있다면 그것을 사용

            message.setTo(userEmail);
            message.setSubject("[결함관리시스템] 임시 비밀번호 발급");

            String emailContent = buildTempPasswordEmailContent(user.getUserName(), tempPassword);
            message.setText(emailContent);

            // 발신자 설정 (application.properties에서 설정된 값 사용)
            message.setFrom("noreply@defectapp.com"); // 설정에 맞게 변경

            javaMailSender.send(message);

            log.info("임시 비밀번호 이메일 전송 완료 - 사용자: {}, 이메일: {}", user.getUserId(), userEmail);

        } catch (MailException e) {
            log.error("임시 비밀번호 이메일 전송 실패 - 사용자: {}, 오류: {}", user.getUserId(), e.getMessage());
            // 이메일 전송 실패 시에도 비밀번호 변경은 완료된 상태이므로 예외를 던지지 않음
            // 필요시 알림이나 로그로 관리자에게 알림
        }
    }

    /**
     * 임시 비밀번호 이메일 내용 생성
     * @param userName 사용자 이름
     * @param tempPassword 임시 비밀번호
     * @return 이메일 내용
     */
    private String buildTempPasswordEmailContent(String userName, String tempPassword) {
        StringBuilder content = new StringBuilder();

        content.append("안녕하세요 ").append(userName).append("님,\n\n");
        content.append("결함관리시스템 임시 비밀번호를 발급해드립니다.\n\n");
        content.append("=======================\n");
        content.append("임시 비밀번호: ").append(tempPassword).append("\n");
        content.append("=======================\n\n");
        content.append("보안을 위해 로그인 후 반드시 비밀번호를 변경해주시기 바랍니다.\n\n");
        content.append("발급일시: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        content.append("감사합니다.\n");
        content.append("결함관리시스템 관리자");

        return content.toString();
    }

    public User findByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(UserCode.USER_NOT_FOUND::getUserException);
    }

    public boolean checkUserIdDuplicate(String userId) {
        return userRepository.findByUserId(userId).isPresent();
    }

}