package com.example.customers.infra.rest.filter;

import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * One INFO line per API request: method, path, tenant, status and duration. Never bodies (customer data).
 */
@Slf4j
@Component
class RequestLoggingFilter extends OncePerRequestFilter {

  private static final String TENANT_HEADER = "X-Tenant-Id";
  private static final String ACTUATOR_PATH = "/actuator";

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith(ACTUATOR_PATH);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var startNanos = System.nanoTime();

    try {
      filterChain.doFilter(request, response);
    } finally {
      var endNanos = System.nanoTime();
      var elapsedMillis = NANOSECONDS.toMillis(endNanos - startNanos);
      String method = request.getMethod();
      String target = toTarget(request);
      String tenantId = request.getHeader(TENANT_HEADER);
      var status = response.getStatus();
      log.info("{} {} tenant={} -> {} ({} ms)", method, target, tenantId, status, elapsedMillis);
    }
  }

  private static String toTarget(HttpServletRequest request) {
    String path = request.getRequestURI();
    String query = request.getQueryString();
    return nonNull(query) ? path + "?" + query : path;
  }
}
