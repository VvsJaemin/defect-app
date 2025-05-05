package com.group.defectapp.repository.defectlog.search;

import com.group.defectapp.dto.defectlog.DefectLogListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DefectLogSearch {

    Page<DefectLogListDto> list(Pageable pageable, String defectId);

}
