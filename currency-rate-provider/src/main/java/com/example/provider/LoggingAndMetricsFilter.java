package com.example.provider;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class LoggingAndMetricsFilter extends OncePerRequestFilter {

    public static final String CLIENT_NAME_HEADER = "X-Client-Name";
    private static final Logger log = LoggerFactory.getLogger(LoggingAndMetricsFilter.class);
    private static final String REQUEST_METRIC = "rpc.server.requests";
    private static final String ERROR_METRIC = "rpc.server.errors";

    private final Optional<MeterRegistry> meterRegistry;

    public LoggingAndMetricsFilter(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistry = Optional.ofNullable(meterRegistryProvider.getIfAvailable());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        Timer.Sample sample = meterRegistry.map(Timer::start).orElse(null);
        String clientName = resolveClientName(request);
        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
            status = responseWrapper.getStatus();
        } catch (Exception exception) {
            status = resolveStatus(exception);
            throw exception;
        } finally {
            logRequest(requestWrapper, clientName);
            logResponse(requestWrapper, responseWrapper, clientName, status);
            recordMetrics(requestWrapper, clientName, status, sample);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void recordMetrics(HttpServletRequest request, String clientName, int status, Timer.Sample sample) {
        if (meterRegistry.isEmpty() || sample == null) {
            return;
        }

        List<Tag> tags = List.of(
                Tag.of("client", clientName),
                Tag.of("method", request.getMethod()),
                Tag.of("uri", request.getRequestURI()),
                Tag.of("status", String.valueOf(status))
        );

        sample.stop(Timer.builder(REQUEST_METRIC)
                .description("JSON-RPC request processing time")
                .publishPercentileHistogram()
                .tags(tags)
                .register(meterRegistry.orElseThrow()));

        if (status >= 500) {
            Counter.builder(ERROR_METRIC)
                    .description("Server-side 500 responses")
                    .tags(tags)
                    .register(meterRegistry.orElseThrow())
                    .increment();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, String clientName) {
        log.info("server request client={} method={} uri={} body={}",
                clientName,
                request.getMethod(),
                request.getRequestURI(),
                readBody(request.getContentAsByteArray()));
    }

    private void logResponse(ContentCachingRequestWrapper request,
                             ContentCachingResponseWrapper response,
                             String clientName,
                             int status) {
        log.info("server response client={} method={} uri={} status={} body={}",
                clientName,
                request.getMethod(),
                request.getRequestURI(),
                status,
                readBody(response.getContentAsByteArray()));
    }

    private String readBody(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "<empty>";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String resolveClientName(HttpServletRequest request) {
        String clientName = request.getHeader(CLIENT_NAME_HEADER);
        return clientName == null || clientName.isBlank() ? "unknown" : clientName;
    }

    private int resolveStatus(Exception exception) {
        if (exception instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode().value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}
