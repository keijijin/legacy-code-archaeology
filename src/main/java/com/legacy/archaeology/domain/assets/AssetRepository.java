package com.legacy.archaeology.domain.assets;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<AssetEntity, Long> {

    Optional<AssetEntity> findByAssetId(String assetId);

    List<AssetEntity> findAllByProjectId(String projectId);

    boolean existsByProjectIdAndSourcePathAndVersionHash(
            String projectId, String sourcePath, String versionHash);
}
