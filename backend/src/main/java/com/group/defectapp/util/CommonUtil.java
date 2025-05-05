package com.group.defectapp.util;

import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;

@Component
@Slf4j
public class CommonUtil {

    /**
     * 시스템에 쿠키로 설정된 language 값
     * @return 쿠키값
     */
    public static Locale getLocale(){
        ServletRequestAttributes servletRequestAttribute = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        Cookie[] cookies = servletRequestAttribute.getRequest().getCookies();   // 모든 쿠키 가져오기
        String locale = "";
        if (cookies != null) {
            for (Cookie c : cookies) {
                String name = c.getName();   // 쿠키 이름 가져오기
                String value = c.getValue(); // 쿠키 값 가져오기
                if (StringUtils.equals(name, "lang")) {
                    locale = StringUtils.replace(value,"-","_");
                    break;
                }
            }
        }


        if (StringUtils.isEmpty(locale)) {
            locale = String.valueOf(Locale.KOREA);
        }
        return LocaleUtils.toLocale(locale);
    }

}
