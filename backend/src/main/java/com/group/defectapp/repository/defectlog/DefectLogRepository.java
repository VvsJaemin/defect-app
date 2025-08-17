package com.group.defectapp.repository.defectlog;

import com.group.defectapp.domain.defectlog.DefectLog;
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

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM defect_log_files_m WHERE log_seq IN (SELECT dl.log_seq FROM tb_defect_log_m dl WHERE dl.defect_id IN (SELECT def.defect_id FROM tb_defect_m def WHERE def.project_id IN :projectIds))", nativeQuery = true)
    void deleteAllDefectLogFileByProjectIdIn(@Param("projectIds") List<String> projectIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM DefectLog dl WHERE dl.defectId IN (SELECT d.defectId FROM Defect d WHERE d.projectId IN :projectIds)")
    void deleteAllByProjectIdIn(@Param("projectIds") List<String> projectIds);
}