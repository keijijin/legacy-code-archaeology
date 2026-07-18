package com.legacy.archaeology.domain.assets;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 取込資産エンティティ。プロジェクトに紐付く解析対象ファイルやリポジトリを表す。 */
@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
public class AssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false, unique = true, length = 20)
    private String assetId;

    @Column(name = "project_id", nullable = false, length = 20)
    private String projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 50)
    private AssetType assetType;

    @Column(name = "source_path", columnDefinition = "text")
    private String sourcePath;

    @Column(name = "version_hash", length = 64)
    private String versionHash;

    @Column(name = "imported_at", nullable = false)
    private OffsetDateTime importedAt;

    @PrePersist
    void onCreate() {
        this.importedAt = OffsetDateTime.now();
    }
}
