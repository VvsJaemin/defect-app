package com.group.defectapp.repository.user.search;

import com.group.defectapp.dto.defect.DefectListDto;
import com.group.defectapp.dto.user.UserListDto;
import com.group.defectapp.dto.user.UserResponseDto;
import com.group.defectapp.dto.user.UserSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserSearch {

    Page<UserListDto> list(UserSearchCondition condition, Pageable pageable);

}
