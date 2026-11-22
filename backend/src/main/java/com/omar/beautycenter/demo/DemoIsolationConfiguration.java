package com.omar.beautycenter.demo;

import java.net.URI;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@Profile("demo")
public class DemoIsolationConfiguration {

    @Bean
    static BeanFactoryPostProcessor demoDatabaseSafetyCheck(Environment environment) {
        return beanFactory -> validateConnection(
                environment.getProperty("spring.datasource.url", ""),
                environment.getProperty("spring.datasource.username", ""));
    }

    static void validateConnection(String url, String username) {
        validateConnection(url, username, System.getenv("DEMO_ALLOWED_DB_HOST"));
    }

    static void validateConnection(String url, String username, String allowedRemoteHost) {
        boolean safe = false;

        try {
            if (url != null && url.startsWith("jdbc:postgresql://")) {
                URI uri = URI.create(url.substring(5));
                String host = uri.getHost();

                boolean local = "127.0.0.1".equals(host)
                        || "localhost".equalsIgnoreCase(host);

                boolean approvedRemote = allowedRemoteHost != null
                        && !allowedRemoteHost.isBlank()
                        && allowedRemoteHost.equalsIgnoreCase(host);

                boolean querySafe = local
                        ? uri.getQuery() == null || "sslmode=require".equals(uri.getQuery())
                        : "sslmode=require".equals(uri.getQuery());

                boolean portSafe = local
                        ? uri.getPort() == -1 || uri.getPort() == 5432 || uri.getPort() == 5433
                        : uri.getPort() == 5432;

                safe = "/beauty_center_demo".equals(uri.getPath())
                        && uri.getUserInfo() == null
                        && uri.getFragment() == null
                        && querySafe
                        && portSafe
                        && (local || approvedRemote)
                        && "beauty_demo_app".equals(username);
            }
        } catch (IllegalArgumentException ignored) {
        }

        if (!safe) {
            throw new IllegalStateException(
                    "Demo startup refused: database, role, host or SSL validation failed.");
        }
    }
}
