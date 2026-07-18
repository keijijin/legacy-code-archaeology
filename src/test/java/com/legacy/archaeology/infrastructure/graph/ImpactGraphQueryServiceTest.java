package com.legacy.archaeology.infrastructure.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.Neo4jClient;

@ExtendWith(MockitoExtension.class)
class ImpactGraphQueryServiceTest {

    @Mock private Neo4jClient neo4jClient;

    @InjectMocks private ImpactGraphQueryService service;

    @Test
    void WRITEの直接依存はHIGHになること() {
        String level = service.resolveImpactLevel(1, List.of("RELATION:WRITES"));
        assertThat(level).isEqualTo("HIGH");
    }

    @Test
    void 中程度の深さはMEDIUMになること() {
        String level = service.resolveImpactLevel(2, List.of("RELATION:READS"));
        assertThat(level).isEqualTo("MEDIUM");
    }

    @Test
    void 深い依存はLOWになること() {
        String level = service.resolveImpactLevel(3, List.of("RELATION:CALLS"));
        assertThat(level).isEqualTo("LOW");
    }
}
