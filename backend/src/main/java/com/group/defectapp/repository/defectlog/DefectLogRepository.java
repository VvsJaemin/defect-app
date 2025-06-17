package com.group.defectapp.repository.defectlog;

import com.group.defectapp.domain.defectlog.DefectLog;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.repository.defectlog.search.DefectLogSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DefectLogRepository extends JpaRepository<DefectLog, Integer>, DefectLogSearch {

    Optional<DefectLog> findByDefectId(String defectId);

    List<DefectLog> findAllByDefectId(String defectId);


    void deleteByDefectId(String defectId);

    @Modifying
    @Query("UPDATE DefectLog d SET d.logCt = :logCt WHERE d.defectId = :defectId and d.statusCd ='DS1000'")
    void updateDefectLogCt(@Param("defectId") String defectId, @Param("logCt") String logCt);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM defect_log_files_m WHERE log_defect_id = :defectId AND idx IN (:idx)", nativeQuery = true)
    void deleteDefectLogFile(@Param("defectId") String defectId, @Param("idx") List<String> idx);
}
