package com.example.printer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class ClientHttpLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ClientHttpLoggingInterceptor.class);
    private static final String CLIENT_NAME_HEADER = "X-Client-Name";

    private final String clientName;

    public ClientHttpLoggingInterceptor(@Value("${rate-printer.client-name}") String clientName) {
        this.clientName = clientName;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().set(CLIENT_NAME_HEADER, clientName);
        log.info("client request client={} method={} uri={} body={}",
                clientName,
                request.getMethod(),
                request.getURI(),
                readBody(body));

        ClientHttpResponse response = execution.execute(request, body);
        byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());

        log.info("client response client={} method={} uri={} status={} body={}",
                clientName,
                request.getMethod(),
                request.getURI(),
                response.getStatusCode().value(),
                readBody(responseBody));

        return new ReusableClientHttpResponse(response, responseBody);
    }

    public BufferingClientHttpRequestFactory bufferingFactory(org.springframework.http.client.ClientHttpRequestFactory delegate) {
        return new BufferingClientHttpRequestFactory(delegate);
    }

    private String readBody(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "<empty>";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
