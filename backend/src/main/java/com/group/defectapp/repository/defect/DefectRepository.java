package com.group.defectapp.repository.defect;

import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.dto.defect.DefectResponseDto;
import com.group.defectapp.repository.defect.search.DefectSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

}