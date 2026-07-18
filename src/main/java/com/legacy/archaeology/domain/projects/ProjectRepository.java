package com.legacy.archaeology.domain.projects;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    Optional<ProjectEntity> findByProjectId(String projectId);

    boolean existsByProjectId(String projectId);
}
