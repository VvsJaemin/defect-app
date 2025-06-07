package com.group.defectapp.repository.defectlog;

import com.group.defectapp.domain.defectlog.DefectLog;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.repository.defectlog.search.DefectLogSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DefectLogRepository extends JpaRepository<DefectLog, Integer>, DefectLogSearch {

    Optional<DefectLog> findByDefectId(String defectId);

    void deleteByDefectId(String defectId);

    @Modifying
    @Query("UPDATE DefectLog d SET d.logCt = :logCt WHERE d.defectId = :defectId and d.statusCd ='DS1000'")
    void updateDefectLogCt(@Param("defectId") String defectId, @Param("logCt") String logCt);
}
