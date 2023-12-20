package com.codemaniac.codemaniacrestclientlibrary;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LoggingService {
    static final String SECURITY_PROPERTY = "TOKEN";
    private static final char LESS_THAN_BRACKET = '<';
    static final String JWT_CONTENT_SUB_TYPE = "jwt";

    static final List<String> TRACE_HEADERS = Arrays.asList("SPAN_ID", "TRACE_ID");

    private static final Set<String> SENSITIVE_HEADERS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("Authorization", "Cookie"))
    );

    private Map<String, String> buildRequestHeadersMap(HttpRequest request) {
        Map<String, String> map = new HashMap<>();
        TRACE_HEADERS.forEach(header -> setSpanTraceIdHeaders(header, request, map));
        return map;
    }

    private void setSpanTraceIdHeaders(String header, HttpRequest request, Map<String, String> map) {
        List<String> values = request.getHeaders().get(header);
        String val = StringUtils.EMPTY;

        if (CollectionUtils.isNotEmpty(values)) {
            val = values.get(0);
        }

        if (StringUtils.isBlank(val)) {
            val = UUID.randomUUID().toString();
        }

        map.put(header, val);
    }

    void buildReqLogger(HttpRequest httpRequest, String body) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("REQUEST ");
        stringBuilder.append("method=[").append(httpRequest.getMethod()).append("] ");
        stringBuilder.append("path=[").append(httpRequest.getURI().toString()).append("] ");
        stringBuilder.append("headers=[").append(maskSensitiveHeaders(httpRequest.getHeaders())).append("] ");

        if (StringUtils.isNotBlank(body)) {
            if (StringUtils.containsIgnoreCase(body, SECURITY_PROPERTY)) {
                body = removeSecurityTokenFromBody(body);
            }
            stringBuilder.append("body=[").append(body).append("]");
        }
        log.info(stringBuilder.toString());
    }

    private String removeSecurityTokenFromBody(String body) {
        int startIndexOfPassword = StringUtils.indexOf(body, SECURITY_PROPERTY);

        int endIndexOfPassword = startIndexOfPassword;
        while (endIndexOfPassword < body.length() - 1 && body.charAt(endIndexOfPassword) != LESS_THAN_BRACKET) {
            endIndexOfPassword += 1;
        }

        String fullPasswordToRemove = StringUtils.substring(body, startIndexOfPassword, endIndexOfPassword);
        return StringUtils.remove(body, fullPasswordToRemove);
    }


    public void buildResLogger(HttpRequest request, ClientHttpResponse response, long duration) throws IOException {

        String logInfo = "RESPONSE " +
                "method=[" + request.getMethod() + "] " +
                "path=[" + request.getURI().toString() + "] " +
                "responseHeaders=[" + response.getHeaders() + "] " +
                "responseBody=[" + getResponseBody(response) + "] " +
                "Request Duration=[" + duration + "]";

        log.info(logInfo);
        if (getResponseBody(response).isEmpty()) {
            log.warn("Blank response detected");
            // Log as a security event
        }
    }


    public String getResponseBody(ClientHttpResponse response) throws IOException {
        String contentSubType = getContentSubType(response);

        if (StringUtils.containsIgnoreCase(contentSubType, JWT_CONTENT_SUB_TYPE)) {
            return StringUtils.EMPTY; //Do not want to log JWT token
        }

        InputStream nonConvertedBody = response.getBody();
        InputStreamReader isr = new InputStreamReader(
                nonConvertedBody, StandardCharsets.UTF_8);
        return new BufferedReader(isr).lines()
                .collect(Collectors.joining("\n"));
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

    private String getContentSubType(ClientHttpResponse response) {
        HttpHeaders headers = response.getHeaders();
        MediaType contentTypeObj = headers.getContentType();
        if (contentTypeObj != null) {
            return contentTypeObj.getSubtype();
        }

        return StringUtils.EMPTY;
    }
}
