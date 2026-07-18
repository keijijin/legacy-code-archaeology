package com.legacy.archaeology.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.legacy.archaeology.application.dto.OpenShiftMigrationDto;
import com.legacy.archaeology.domain.assets.AssetEntity;
import com.legacy.archaeology.domain.assets.AssetRepository;
import com.legacy.archaeology.domain.assets.AssetType;
import com.legacy.archaeology.domain.projects.ProjectEntity;
import com.legacy.archaeology.domain.projects.ProjectRepository;
import com.legacy.archaeology.infrastructure.graph.ImpactGraphQueryService;
import com.legacy.archaeology.shared.audit.AuditLogger;
import com.legacy.archaeology.shared.id.IdGenerator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtractOpenShiftMigrationIssuesUseCaseTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private ImpactGraphQueryService impactGraphQueryService;
    @Mock private IdGenerator idGenerator;
    @Mock private AuditLogger auditLogger;

    @InjectMocks private ExtractOpenShiftMigrationIssuesUseCase useCase;

    @Test
    void localhost接続はHIGHのNETWORK課題になること() {
        ProjectEntity project = new ProjectEntity();
        project.setProjectId("PRJ-1");
        when(projectRepository.findByProjectId("PRJ-1")).thenReturn(Optional.of(project));
        when(idGenerator.generateEvidenceId()).thenReturn("EV-1");
        when(impactGraphQueryService.findMigrationSignals("PRJ-1"))
                .thenReturn(
                        List.of(
                                Map.of(
                                        "nodeType", "Route",
                                        "nodeId", "ENT-9",
                                        "name", "local-route",
                                        "fromUri", "http://localhost:8080/api",
                                        "sourcePath", "routes/local.xml")));
        when(assetRepository.findAllByProjectId("PRJ-1")).thenReturn(List.of());

        OpenShiftMigrationDto.Response response = useCase.execute("PRJ-1", "tester");

        assertThat(response.getIssueCount()).isEqualTo(1);
        assertThat(response.getIssues().get(0).getCategory()).isEqualTo("NETWORK");
        assertThat(response.getIssues().get(0).getSeverity()).isEqualTo("HIGH");
        assertThat(response.getIssues().get(0).getEvidenceIds()).isNotEmpty();
    }

    @Test
    void Camel資産はINTEGRATION課題として抽出されること() {
        ProjectEntity project = new ProjectEntity();
        project.setProjectId("PRJ-1");
        when(projectRepository.findByProjectId("PRJ-1")).thenReturn(Optional.of(project));
        when(idGenerator.generateEvidenceId()).thenReturn("EV-2");
        when(impactGraphQueryService.findMigrationSignals("PRJ-1")).thenReturn(List.of());

        AssetEntity asset = new AssetEntity();
        asset.setAssetId("AST-1");
        asset.setAssetType(AssetType.CAMEL_ROUTE);
        asset.setSourcePath("routes/customer.xml");
        when(assetRepository.findAllByProjectId("PRJ-1")).thenReturn(List.of(asset));

        OpenShiftMigrationDto.Response response = useCase.execute("PRJ-1", "tester");
        assertThat(response.getIssues()).isNotEmpty();
        assertThat(response.getIssues().get(0).getCategory()).isEqualTo("INTEGRATION");
    }
}
