package com.group.defectapp.repository.defect.search;

import com.group.defectapp.dto.defect.DefectListDto;
import com.group.defectapp.dto.defect.DefectSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DefectSearch {

    Page<DefectListDto> list(Pageable pageable, DefectSearchCondition condition);
}
