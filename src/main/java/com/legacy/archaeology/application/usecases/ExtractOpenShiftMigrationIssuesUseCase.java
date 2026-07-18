package com.legacy.archaeology.application.usecases;

import com.legacy.archaeology.application.dto.OpenShiftMigrationDto;
import com.legacy.archaeology.domain.assets.AssetEntity;
import com.legacy.archaeology.domain.assets.AssetRepository;
import com.legacy.archaeology.domain.assets.AssetType;
import com.legacy.archaeology.domain.projects.ProjectRepository;
import com.legacy.archaeology.infrastructure.graph.ImpactGraphQueryService;
import com.legacy.archaeology.shared.audit.AuditLogger;
import com.legacy.archaeology.shared.id.IdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OpenShift 移行課題抽出ユースケース。
 * グラフ上の接続情報と資産メタデータから移行障壁候補を生成する。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractOpenShiftMigrationIssuesUseCase {

    private final ProjectRepository projectRepository;
    private final AssetRepository assetRepository;
    private final ImpactGraphQueryService impactGraphQueryService;
    private final IdGenerator idGenerator;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public OpenShiftMigrationDto.Response execute(String projectId, String userId) {
        projectRepository
                .findByProjectId(projectId)
                .orElseThrow(
                        () -> new IllegalArgumentException("プロジェクトが見つかりません: " + projectId));

        List<OpenShiftMigrationDto.Issue> issues = new ArrayList<>();

        for (Map<String, Object> signal : impactGraphQueryService.findMigrationSignals(projectId)) {
            issues.add(toIssueFromGraphSignal(signal));
        }

        for (AssetEntity asset : assetRepository.findAllByProjectId(projectId)) {
            issues.addAll(toIssuesFromAsset(asset));
        }

        // 重複タイトルを簡易排除
        List<OpenShiftMigrationDto.Issue> deduped = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (OpenShiftMigrationDto.Issue issue : issues) {
            String key = issue.getCategory() + "|" + issue.getTitle() + "|" + issue.getSourcePath();
            if (!seen.contains(key)) {
                seen.add(key);
                deduped.add(issue);
            }
        }

        auditLogger.log(
                "OPENSHIFT_MIGRATION_ISSUES_EXTRACTED",
                projectId,
                null,
                userId,
                "OpenShiftMigration",
                projectId,
                "issueCount=" + deduped.size());

        return OpenShiftMigrationDto.Response.builder()
                .projectId(projectId)
                .issueCount(deduped.size())
                .issues(deduped)
                .build();
    }

    private OpenShiftMigrationDto.Issue toIssueFromGraphSignal(Map<String, Object> signal) {
        String fromUri = String.valueOf(signal.getOrDefault("fromUri", "")).toLowerCase(Locale.ROOT);
        String sourcePath = String.valueOf(signal.getOrDefault("sourcePath", ""));
        String nodeId = String.valueOf(signal.getOrDefault("nodeId", ""));
        String name = String.valueOf(signal.getOrDefault("name", ""));

        if (fromUri.contains("localhost") || fromUri.contains("127.0.0.1")) {
            return issue(
                    "NETWORK",
                    "HIGH",
                    "localhost / ループバック接続が存在する",
                    name + " が localhost 系エンドポイントを参照しています: " + fromUri,
                    "Service DNS 名または ConfigMap/Secret 経由の外部接続へ置き換える",
                    List.of(nodeId),
                    sourcePath,
                    "LIKELY");
        }
        if (fromUri.startsWith("file:") || sourcePath.toLowerCase(Locale.ROOT).contains("c:\\")) {
            return issue(
                    "STORAGE",
                    "HIGH",
                    "ローカルファイルパス依存がある",
                    name + " がローカルファイルパスに依存しています",
                    "永続ボリュームまたはオブジェクトストレージへ移行し、パスを設定化する",
                    List.of(nodeId),
                    sourcePath,
                    "LIKELY");
        }
        if (fromUri.startsWith("jms:") || fromUri.contains("soap")) {
            return issue(
                    "INTEGRATION",
                    "MEDIUM",
                    "従来型メッセージング/SOAP連携がある",
                    name + " が従来型連携を使用しています: " + fromUri,
                    "Kafka/HTTP API 化と段階移行（Strangler）を検討する",
                    List.of(nodeId),
                    sourcePath,
                    "INFERRED");
        }
        return issue(
                "CONFIGURATION",
                "MEDIUM",
                "OpenShift非適合の可能性のある設定がある",
                name + " に移行時確認が必要な設定があります",
                "設定の外部化とヘルスチェック/プローブ対応を確認する",
                List.of(nodeId),
                sourcePath,
                "INFERRED");
    }

    private List<OpenShiftMigrationDto.Issue> toIssuesFromAsset(AssetEntity asset) {
        List<OpenShiftMigrationDto.Issue> list = new ArrayList<>();
        if (asset.getAssetType() == AssetType.PROPERTIES_CONFIG
                || asset.getAssetType() == AssetType.YAML_CONFIG) {
            list.add(
                    issue(
                            "CONFIGURATION",
                            "LOW",
                            "設定ファイル資産が存在する",
                            "設定資産 " + asset.getSourcePath() + " の外部化状況を確認してください",
                            "Secret/ConfigMap への分離と環境差分管理を行う",
                            List.of(asset.getAssetId()),
                            asset.getSourcePath(),
                            "LIKELY"));
        }
        if (asset.getAssetType() == AssetType.CAMEL_ROUTE) {
            list.add(
                    issue(
                            "INTEGRATION",
                            "MEDIUM",
                            "Camel Route 資産がある",
                            "Route " + asset.getSourcePath() + " のエンドポイント適合性を確認してください",
                            "OpenShift Service/Route への接続先置換とタイムアウト/リトライ設計を行う",
                            List.of(asset.getAssetId()),
                            asset.getSourcePath(),
                            "LIKELY"));
        }
        return list;
    }

    private OpenShiftMigrationDto.Issue issue(
            String category,
            String severity,
            String title,
            String description,
            String recommendation,
            List<String> evidenceIds,
            String sourcePath,
            String confidenceLevel) {
        return OpenShiftMigrationDto.Issue.builder()
                .issueId(idGenerator.generateEvidenceId().replace("EV-", "MI-"))
                .category(category)
                .severity(severity)
                .title(title)
                .description(description)
                .recommendation(recommendation)
                .evidenceIds(evidenceIds)
                .sourcePath(sourcePath)
                .confidenceLevel(confidenceLevel)
                .build();
    }
}
