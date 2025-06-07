package com.group.defectapp.repository.defectlog;

import com.group.defectapp.domain.defectlog.DefectLog;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.repository.defectlog.search.DefectLogSearch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DefectLogRepository extends JpaRepository<DefectLog, Integer>, DefectLogSearch {

    Optional<DefectLog> findByDefectId(String defectId);

    void deleteByDefectId(String defectId);


}
