package com.group.defectapp.repository.defect;

import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.dto.defect.DefectResponseDto;
import com.group.defectapp.repository.defect.search.DefectSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DefectRepository extends JpaRepository<Defect, String>, DefectSearch {

    Optional<Defect> findByDefectId(String defectId);

    @Query("select d from Defect d where d.defectId = :defectId")
    Optional<DefectResponseDto> getDefect(@Param("defectId") String defectId);

    @Query(value = "SELECT defect.get_seq13('DT')", nativeQuery = true)
    String generateDefectIdUsingSequence();

    @Modifying
    @Query("UPDATE Defect d SET d.statusCode = :statusCode WHERE d.defectId = :defectId")
    void updateDefectStatusCode(@Param("defectId") String defectId, @Param("statusCode") String statusCode);

    @Modifying
    @Query("UPDATE Defect d SET d.assignee = :assignUserId WHERE d.defectId = :defectId")
    void updateDefectAssignUserId(@Param("assignUserId") String assignUserId, @Param("defectId") String defectId);

    @Modifying
    @Query("UPDATE Defect d SET d.openYn = 'N' WHERE d.defectId = :defectId")
    void deleteDefect(@Param("defectId") String defectId);

    @Modifying
    void deleteAllByProjectIdIn(List<String> projectIds);

    // 오늘 발생 결함 수 - 삭제되지 않은 결함만 계산
    @Query("SELECT COUNT(d) FROM Defect d WHERE FUNCTION('DATE', d.createdAt) = CURRENT_DATE AND d.openYn = 'Y'")
    long countTodayDefect();

    // 오늘 처리 결함 수 (DS5000) - 삭제되지 않은 결함만 계산
    @Query("SELECT COUNT(d) FROM Defect d WHERE FUNCTION('DATE', d.updatedAt) = CURRENT_DATE AND d.statusCode = 'DS5000' AND d.openYn = 'Y'")
    long countTodayProcessedDefect();

    // 누적 총 결함 수 - 삭제되지 않은 결함만 계산
    @Query("SELECT COUNT(d) FROM Defect d WHERE d.openYn = 'Y'")
    long countTotalDefect();

    // 결함 해제 수 (DS5000) - 삭제되지 않은 결함만 계산
    @Query("SELECT COUNT(d) FROM Defect d WHERE d.statusCode = 'DS5000' AND d.openYn = 'Y'")
    long countDefectCanceled();

    // 결함 종료 수 (DS6000) - 상태코드 수정 및 삭제되지 않은 결함만 계산
    @Query("SELECT COUNT(d) FROM Defect d WHERE d.statusCode = 'DS6000' AND d.openYn = 'Y'")
    long countDefectClosed();

    /**
     * 특정 사용자가 담당자로 할당된 결함들의 담당자를 NULL로 변경 - 삭제되지 않은 결함만 대상
     */
    @Modifying
    @Query("UPDATE Defect d SET d.assignee = null WHERE d.assignee = :userId AND d.openYn = 'Y'")
    int updateAssigneeToNull(@Param("userId") String userId);

    /**
     * 특정 사용자가 담당자로 할당된 결함 개수 조회 - 삭제되지 않은 결함만 계산
     */
    @Query("SELECT COUNT(d) FROM Defect d WHERE d.assignee = :userId AND d.openYn = 'Y'")
    long countByAssignee(@Param("userId") String userId);

    @Query(value = """
    SELECT
      :startDate as startDate,
      :endDate as endDate,
      DATE(d.first_reg_dtm) AS defect_date,
      COUNT(d.defect_id) AS total_defects,
      SUM(CASE WHEN d.status_cd = 'DS5000' THEN 1 ELSE 0 END) AS canceled_defects,
      SUM(CASE WHEN d.status_cd = 'DS6000' THEN 1 ELSE 0 END) AS closed_defects,
      SUM(CASE WHEN d.status_cd NOT IN ('DS5000', 'DS6000') THEN 1 ELSE 0 END) AS active_defects
    FROM tb_defect_m d
    WHERE d.first_reg_dtm >= :startDate
      AND d.first_reg_dtm < DATE_ADD(:endDate, INTERVAL 1 DAY)
      AND d.open_yn = 'Y'
    GROUP BY DATE(d.first_reg_dtm)
    ORDER BY defect_date
""", nativeQuery = true)
    List<Map<String, Object>> findWeeklyDefectStats(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

}