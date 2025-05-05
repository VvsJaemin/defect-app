package com.group.defectapp.repository.project.search;

import com.group.defectapp.dto.project.ProjectResponseDto;
import com.group.defectapp.dto.project.ProjectSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectSearch {

    Page<ProjectResponseDto> list(Pageable pageable, ProjectSearchCondition condition);

}
