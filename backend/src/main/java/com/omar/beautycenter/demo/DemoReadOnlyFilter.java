package com.omar.beautycenter.demo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Demo-only business write barrier. Login retains existing audit/lockout behavior. */
@Component
@Profile("demo")
@Order(-90)
public class DemoReadOnlyFilter extends OncePerRequestFilter {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final Set<String> PREVIEW_FIELDS = Set.of("configurationId", "quantity", "discountId");
    private final boolean readOnly;
    public DemoReadOnlyFilter(@Value("${app.demo.read-only:true}") boolean readOnly) { this.readOnly = readOnly; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Beauty-Demo", readOnly ? "read-only" : "local-seeding");
        String path = request.getServletPath().isEmpty() ? request.getRequestURI() : request.getServletPath();
        boolean login = "POST".equals(request.getMethod()) && "/api/v1/auth/login".equals(path);
        if (readOnly && !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod()) && !login) {
            // The existing preview is pure unless it contains a price override (which writes audit rows).
            // Permit only its three read-only inputs; never pass override fields to that service.
            if ("POST".equals(request.getMethod()) && "/api/v1/hair-configurations/preview".equals(path)) {
                byte[] body = request.getInputStream().readNBytes(4097);
                if (safePreview(body)) {
                    chain.doFilter(replay(request, body), response);
                    return;
                }
            }
            response.setStatus(403);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":403,\"code\":\"DEMO_READ_ONLY\",\"message\":\"دي نسخة للعرض فقط؛ الحفظ والتعديل غير متاحين.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean safePreview(byte[] body) {
        if (body.length > 4096) return false;
        try {
            var json = JSON.readTree(body);
            if (json == null || !json.isObject()) return false;
            var fields = json.fieldNames();
            while (fields.hasNext()) if (!PREVIEW_FIELDS.contains(fields.next())) return false;
            return true; // Existing request validation still checks required fields and values.
        } catch (IOException invalidJson) { return false; }
    }

    private static HttpServletRequest replay(HttpServletRequest request, byte[] body) {
        return new HttpServletRequestWrapper(request) {
            @Override public ServletInputStream getInputStream() {
                var input = new ByteArrayInputStream(body);
                return new ServletInputStream() {
                    @Override public int read() { return input.read(); }
                    @Override public boolean isFinished() { return input.available() == 0; }
                    @Override public boolean isReady() { return true; }
                    @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException(); }
                };
            }
        };
    }
}
