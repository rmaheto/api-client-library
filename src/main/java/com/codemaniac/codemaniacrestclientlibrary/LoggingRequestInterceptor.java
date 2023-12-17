package com.codemaniac.codemaniacrestclientlibrary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoggingRequestInterceptor.class);

    // A set of headers that contain sensitive information
    private static final Set<String> SENSITIVE_HEADERS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("Authorization", "Cookie"))
    );

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        logRequest(request, body);
        long startTime = System.currentTimeMillis();
        ClientHttpResponse response = execution.execute(request, body);
        long endTime = System.currentTimeMillis();
        logResponse(response, endTime - startTime);
        return response;
    }

    private void logRequest(HttpRequest request, byte[] body) throws IOException {
        log.info("Request URI: {}", request.getURI());
        log.info("Request Method: {}", request.getMethod());
        log.info("Request Body: {}", new String(body, StandardCharsets.UTF_8));
        log.info("Request Headers: {}", maskSensitiveHeaders(request.getHeaders()));
    }

    private void logResponse(ClientHttpResponse response, long duration) throws IOException {
        log.info("Response Status Code: {}", response.getStatusCode());
        log.info("Response Headers: {}", maskSensitiveHeaders(response.getHeaders()));
        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        log.info("Response Body: {}", responseBody);
        log.info("Request Duration: {} ms", duration);

        if (responseBody.isEmpty()) {
            log.warn("Blank response detected");
            // Log as a security event
        }
    }

    private HttpHeaders maskSensitiveHeaders(HttpHeaders originalHeaders) {
        HttpHeaders maskedHeaders = new HttpHeaders();
        originalHeaders.forEach((name, values) -> {
            if (SENSITIVE_HEADERS.contains(name)) {
                maskedHeaders.add(name, "****");
            } else {
                maskedHeaders.addAll(name, values);
            }
        });
        return maskedHeaders;
    }
}
