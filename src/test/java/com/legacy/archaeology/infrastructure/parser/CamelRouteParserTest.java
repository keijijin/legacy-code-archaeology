package com.legacy.archaeology.infrastructure.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CamelRouteParserTest {

    private final CamelRouteParser parser = new CamelRouteParser();

    @Test
    void Camelルートのfromとtoとbeanを抽出できること() {
        String xml =
                """
                <camelContext xmlns="http://camel.apache.org/schema/spring">
                  <route id="customer-registration-route">
                    <from uri="jms:queue:customer.in"/>
                    <bean ref="customerService"/>
                    <to uri="http://notification/api/send"/>
                  </route>
                </camelContext>
                """;

        List<CamelRouteParser.ParsedRoute> routes = parser.parseContent(xml, "test.xml");

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getRouteId()).isEqualTo("customer-registration-route");
        assertThat(routes.get(0).getFromUri()).isEqualTo("jms:queue:customer.in");
        assertThat(routes.get(0).getSteps()).contains("bean:customerService");
        assertThat(routes.get(0).getSteps()).contains("to:http://notification/api/send");
    }
}
