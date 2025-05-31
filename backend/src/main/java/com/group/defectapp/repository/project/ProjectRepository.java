package com.group.defectapp.repository.project;

import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.repository.project.search.ProjectSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, String>, ProjectSearch {

    Optional<Project> findByProjectId(String projectId);


    @Query(value = "SELECT defect.get_seq10('PROJ')", nativeQuery = true)
    String generateProjectIdUsingSequence();

    @Modifying
    @Query("DELETE FROM Project p WHERE p.projectId in :projectIds ")
    void deleteAllByIdIn(List<String> projectIds);
}
