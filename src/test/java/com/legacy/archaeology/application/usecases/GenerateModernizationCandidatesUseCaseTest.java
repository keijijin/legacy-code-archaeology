package com.legacy.archaeology.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.legacy.archaeology.application.dto.ModernizationDto;
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
class GenerateModernizationCandidatesUseCaseTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ImpactGraphQueryService impactGraphQueryService;
    @Mock private IdGenerator idGenerator;
    @Mock private AuditLogger auditLogger;

    @InjectMocks private GenerateModernizationCandidatesUseCase useCase;

    @Test
    void 結合度ゼロはRETIRE候補になること() {
        ProjectEntity project = new ProjectEntity();
        project.setProjectId("PRJ-1");
        when(projectRepository.findByProjectId("PRJ-1")).thenReturn(Optional.of(project));
        when(idGenerator.generateBusinessRuleId()).thenReturn("BR-1");
        when(impactGraphQueryService.findModernizationTargets("PRJ-1"))
                .thenReturn(
                        List.of(
                                Map.of(
                                        "nodeType", "Program",
                                        "nodeId", "ENT-1",
                                        "name", "UnusedService",
                                        "sourcePath", "a.java",
                                        "fromUri", "",
                                        "degree", 0)));

        ModernizationDto.Response response = useCase.execute("PRJ-1", "tester");

        assertThat(response.getCandidateCount()).isEqualTo(1);
        assertThat(response.getCandidates().get(0).getAction()).isEqualTo("RETIRE");
        assertThat(response.getCandidates().get(0).getEvidenceIds()).contains("ENT-1");
    }

    @Test
    void 高結合はREDESIGN候補になること() {
        ProjectEntity project = new ProjectEntity();
        project.setProjectId("PRJ-1");
        when(projectRepository.findByProjectId(anyString())).thenReturn(Optional.of(project));
        when(idGenerator.generateBusinessRuleId()).thenReturn("BR-2");
        when(impactGraphQueryService.findModernizationTargets("PRJ-1"))
                .thenReturn(
                        List.of(
                                Map.of(
                                        "nodeType", "Route",
                                        "nodeId", "ENT-2",
                                        "name", "heavy-route",
                                        "sourcePath", "route.xml",
                                        "fromUri", "jms:queue:in",
                                        "degree", 12)));

        ModernizationDto.Response response = useCase.execute("PRJ-1", "tester");
        assertThat(response.getCandidates().get(0).getAction()).isEqualTo("REDESIGN");
        assertThat(response.getCandidates().get(0).getPriority()).isEqualTo("HIGH");
    }
}
