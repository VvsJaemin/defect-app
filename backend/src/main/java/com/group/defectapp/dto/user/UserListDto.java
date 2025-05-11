package com.group.defectapp.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.group.defectapp.domain.user.User;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserListDto implements Serializable {

    private String userId;
    private String userName;
    private String userSeCd;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime first_reg_dtm;


}