package com.group.defectapp.validation;

import com.group.defectapp.annotation.ConditionalEmail;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class ConditionalEmailValidator implements ConstraintValidator<ConditionalEmail, String> {

    @Override
    public void initialize(ConditionalEmail constraintAnnotation) {
        // 초기화 로직이 필요하면 여기에 구현
    }

    @Override
    public boolean isValid(String userId, ConstraintValidatorContext context) {
        if (userId == null || userId.trim().isEmpty()) {
            return false; // null이나 빈 문자열은 @NotBlank에서 처리
        }

        // admin인 경우 이메일 검증을 건너뛰고 true 반환
        if(StringUtils.equalsAny(userId.trim(),"admin", "test", "devtest")){
            return true;
        }
        // admin이 아닌 경우 이메일 형식 검증
        return userId.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}