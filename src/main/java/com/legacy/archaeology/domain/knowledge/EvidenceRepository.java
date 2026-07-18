package com.legacy.archaeology.domain.knowledge;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceRepository extends JpaRepository<EvidenceEntity, Long> {

    Optional<EvidenceEntity> findByEvidenceId(String evidenceId);

    List<EvidenceEntity> findAllByProjectId(String projectId);

    List<EvidenceEntity> findAllBySourceAssetId(String sourceAssetId);
}
