package com.omar.beautycenter.backup;

import com.omar.beautycenter.common.error.BusinessRuleException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {
    private static final Set<String> WRITE_METHODS = Set.of(HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.PATCH.name(), HttpMethod.DELETE.name());
    private final MaintenanceMode maintenanceMode;
    public MaintenanceModeFilter(MaintenanceMode maintenanceMode) { this.maintenanceMode = maintenanceMode; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (maintenanceMode.isWriteBlocked() && WRITE_METHODS.contains(request.getMethod())
                && !request.getRequestURI().startsWith("/api/v1/backups")) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"النظام مشغول باستعادة نسخة احتياطية. حاول مرة أخرى بعد قليل.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
