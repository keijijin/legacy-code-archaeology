package com.legacy.archaeology.application.dto;

import com.legacy.archaeology.domain.assets.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

/** 資産取込関連 DTO */
public class AssetDto {

    @Getter
    @Builder
    public static class IngestRequest {
        @NotNull(message = "資産種別は必須です")
        private AssetType assetType;

        @NotBlank(message = "ソースパスまたはURLは必須です")
        private String sourcePath;
    }

    @Getter
    @Builder
    public static class Response {
        private String assetId;
        private String projectId;
        private AssetType assetType;
        private String sourcePath;
        private String versionHash;
        private OffsetDateTime importedAt;
    }
}
