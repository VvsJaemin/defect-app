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

    // 오늘 발생 결함 수
    @Query("SELECT COUNT(d) FROM Defect d WHERE FUNCTION('DATE', d.createdAt) = CURRENT_DATE")
    long countTodayDefect();

    // 오늘 처리 결함 수 (DS2000)
    @Query("SELECT COUNT(d) FROM Defect d WHERE FUNCTION('DATE', d.createdAt) = CURRENT_DATE AND d.statusCode = 'DS2000'")
    long countTodayProcessedDefect();

    // 누적 총 결함 수
    @Query("SELECT COUNT(d) FROM Defect d")
    long countTotalDefect();

    // 결함 해제 수 (DS5000)
    @Query("SELECT COUNT(d) FROM Defect d WHERE d.statusCode = 'DS5000'")
    long countDefectCanceled();

    // 결함 종료 수 (DS6000)
    @Query("SELECT COUNT(d) FROM Defect d WHERE d.statusCode = 'DS6000'")
    long countDefectClosed();


    @Query(value = """
    WITH RECURSIVE weeks AS (
      SELECT DATE(:startDate) + INTERVAL (0 - WEEKDAY(:startDate)) DAY AS week_start_date
      UNION ALL
      SELECT week_start_date + INTERVAL 7 DAY
      FROM weeks
      WHERE week_start_date + INTERVAL 7 DAY < :endDate
    )
    
    SELECT
      w.week_start_date,
      COUNT(d.defect_id) AS total_defects,
      SUM(CASE WHEN d.status_cd = 'DS2000' THEN 1 ELSE 0 END) AS completed_defects
    FROM weeks w
    LEFT JOIN tb_defect_m d
      ON d.first_reg_dtm >= w.week_start_date
     AND d.first_reg_dtm < w.week_start_date + INTERVAL 7 DAY
    WHERE w.week_start_date < :endDate
    GROUP BY w.week_start_date
    ORDER BY w.week_start_date
""", nativeQuery = true)
    List<Map<String, Object>> findWeeklyDefectStats(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

}