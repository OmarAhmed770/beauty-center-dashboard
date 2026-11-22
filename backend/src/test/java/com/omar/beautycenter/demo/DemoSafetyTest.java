package com.omar.beautycenter.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DemoSafetyTest {
    @Test void permitsPureHairPreviewWithoutLosingBodyButRejectsOverrideAndAmbiguousJson() throws Exception {
        String body = "{\"configurationId\":1,\"quantity\":50,\"discountId\":null}";
        var request = new MockHttpServletRequest("POST", "/api/v1/hair-configurations/preview");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        var reached = new AtomicBoolean();
        new DemoReadOnlyFilter(true).doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            assertThat(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(body);
            reached.set(true);
        });
        assertThat(reached).isTrue();
        for (String invalid : new String[]{"{\"overrideAmount\":1}", "{\"overrideReason\":\"test\"}",
                "{\"quantity\":1,\"quantity\":2}", "{}{}", "[]", "null", "{", " ".repeat(4097)}) {
            var denied = new MockHttpServletRequest("POST", "/api/v1/hair-configurations/preview");
            denied.setContent(invalid.getBytes(StandardCharsets.UTF_8));
            var response = new MockHttpServletResponse();
            new DemoReadOnlyFilter(true).doFilter(denied, response, (req, res) -> { throw new AssertionError("Unsafe preview passed"); });
            assertThat(response.getStatus()).isEqualTo(403);
        }
    }
    @Test void rejectsClientDatabaseAndInheritedCredentialsBeforeConnecting() {
        DemoIsolationConfiguration.validateConnection("jdbc:postgresql://127.0.0.1:5433/beauty_center_demo", "beauty_demo_app");
        for (String url : new String[]{"jdbc:postgresql://localhost:5433/beauty_center", "jdbc:postgresql://remote/beauty_center_demo", "jdbc:postgresql://localhost/beauty_center_demo?options=bad", "jdbc:h2:mem:demo"}) {
            assertThatThrownBy(() -> DemoIsolationConfiguration.validateConnection(url, "beauty_demo_app")).isInstanceOf(IllegalStateException.class);
        }
        assertThatThrownBy(() -> DemoIsolationConfiguration.validateConnection("jdbc:postgresql://localhost/beauty_center_demo", "beautycenter")).isInstanceOf(IllegalStateException.class);
    }
    @Test void permitsOnlyExplicitlyApprovedRemoteTargetWithoutChangingLocalSupport() {
        String host = "demo-db.example.invalid";
        DemoIsolationConfiguration.validateConnection(
                "jdbc:postgresql://demo-db.example.invalid:5432/beauty_center_demo?sslmode=require",
                "beauty_demo_app", host);
        DemoIsolationConfiguration.validateConnection(
                "jdbc:postgresql://DEMO-DB.EXAMPLE.INVALID:5432/beauty_center_demo?sslmode=require",
                "beauty_demo_app", host);
        DemoIsolationConfiguration.validateConnection(
                "jdbc:postgresql://127.0.0.1:5433/beauty_center_demo", "beauty_demo_app", null);
        DemoIsolationConfiguration.validateConnection(
                "jdbc:postgresql://localhost:5432/beauty_center_demo?sslmode=require", "beauty_demo_app", host);
    }
    @Test void rejectsRemoteHostDatabaseRoleTlsAndUrlOverridesBeforeConnecting() {
        String host = "demo-db.example.invalid";
        String valid = "jdbc:postgresql://demo-db.example.invalid:5432/beauty_center_demo?sslmode=require";
        for (String allowedHost : new String[]{null, "", " ", "different.example.invalid", "example.invalid"}) {
            assertThatThrownBy(() -> DemoIsolationConfiguration.validateConnection(valid, "beauty_demo_app", allowedHost))
                    .isInstanceOf(IllegalStateException.class);
        }
        for (String url : new String[]{null, "", "jdbc:h2:mem:demo",
                valid.replace(host, "unapproved.example.invalid"),
                valid.replace(host, host + ".other.invalid"),
                valid.replace("beauty_center_demo", "beauty_center"),
                valid.replace("beauty_center_demo", "postgres"),
                valid.replace(":5432", ":5433"), valid.replace(":5432", ""),
                valid.replace("?sslmode=require", ""), valid.replace("sslmode=require", "sslmode=disable"),
                valid + "&options=unapproved", valid + "&sslmode=disable", valid + "#fragment",
                valid.replace("//", "//unexpected-user@")}) {
            assertThatThrownBy(() -> DemoIsolationConfiguration.validateConnection(url, "beauty_demo_app", host))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Demo startup refused: database, role, host or SSL validation failed.");
        }
        for (String role : new String[]{null, "", "beautycenter", "postgres"}) {
            assertThatThrownBy(() -> DemoIsolationConfiguration.validateConnection(valid, role, host))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
    @Test void blocksAllBusinessWritesIncludingOwnerBackupRestoreAndRegistration() throws Exception {
        for (String path : new String[]{"/api/v1/invoices/quick-orders", "/api/v1/backups/1/restore", "/api/v1/backups/manual", "/api/v1/auth/register", "/api/v1/setup/owner", "/api/v1/employees/1/permissions"}) {
            for (String method : new String[]{"POST", "PUT", "PATCH", "DELETE"}) {
                var response = new MockHttpServletResponse();
                new DemoReadOnlyFilter(true).doFilter(new MockHttpServletRequest(method, path), response, (req, res) -> { throw new AssertionError("Write reached controller"); });
                assertThat(response.getStatus()).isEqualTo(403);
                assertThat(response.getContentAsString()).contains("DEMO_READ_ONLY");
            }
        }
    }
    @Test void permitsReadingLoginAndExplicitLocalSeedingMode() throws Exception {
        for (String[] call : new String[][]{{"GET", "/api/v1/invoices"}, {"POST", "/api/v1/auth/login"}, {"OPTIONS", "/api/v1/auth/login"}}) {
            var reached = new AtomicBoolean();
            new DemoReadOnlyFilter(true).doFilter(new MockHttpServletRequest(call[0], call[1]), new MockHttpServletResponse(), (req, res) -> reached.set(true));
            assertThat(reached).isTrue();
        }
        var reached = new AtomicBoolean();
        new DemoReadOnlyFilter(false).doFilter(new MockHttpServletRequest("POST", "/api/v1/invoices/quick-orders"), new MockHttpServletResponse(), (req, res) -> reached.set(true));
        assertThat(reached).isTrue();
    }
}
