package com.omar.beautycenter.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Loads the real application-demo.yml and Spring @Value binding, without any database. */
class DemoConfigurationTest {
    private ApplicationContextRunner configuredWith(Map<String, Object> variables) {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().getPropertySources().replace(
                        "systemEnvironment", new SystemEnvironmentPropertySource("systemEnvironment", variables)))
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=demo")
                .withUserConfiguration(FilterOnlyConfiguration.class);
    }

    @Test void missingEnvironmentFlagDefaultsToReadOnly() {
        verify(configuredWith(Map.of()), true);
    }

    @Test void falseEnvironmentFlagEnablesExplicitSeedingMode() {
        verify(configuredWith(Map.of("DEMO_READ_ONLY", "false")), false);
    }

    @Test void trueEnvironmentFlagKeepsWriteBarrier() {
        verify(configuredWith(Map.of("DEMO_READ_ONLY", "true")), true);
    }

    @Test void jvmPropertyTakesPrecedenceOverYamlEnvironmentPlaceholder() {
        verify(configuredWith(Map.of("DEMO_READ_ONLY", "true"))
                .withSystemProperties("app.demo.read-only=false"), false);
    }

    private void verify(ApplicationContextRunner runner, boolean readOnly) {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(DemoReadOnlyFilter.class);
            assertThat(context.getEnvironment().getProperty("app.demo.read-only", Boolean.class)).isEqualTo(readOnly);
            var filter = context.getBean(DemoReadOnlyFilter.class);
            var response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/setup/status"), response,
                    (request, result) -> { });
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-Beauty-Demo")).isEqualTo(readOnly ? "read-only" : "local-seeding");

            var reached = new AtomicBoolean();
            var writeResponse = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("POST", "/api/v1/setup/owner"), writeResponse,
                    (request, result) -> reached.set(true));
            assertThat(reached.get()).isEqualTo(!readOnly);
            assertThat(writeResponse.getStatus()).isEqualTo(readOnly ? 403 : 200);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(DemoReadOnlyFilter.class)
    static class FilterOnlyConfiguration { }
}
