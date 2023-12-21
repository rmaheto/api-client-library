package com.codemaniac.codemaniacrestclientlibrary;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

@Component
@Slf4j
public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {
    @Autowired
    private LoggingService loggingService;


    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        loggingService.buildReqLogger(request, Arrays.toString(body));
        long startTime = System.currentTimeMillis();
        ClientHttpResponse originalResponse = execution.execute(request, body);

        BufferingClientHttpResponseWrapper responseWrapper = new BufferingClientHttpResponseWrapper(originalResponse);
        long endTime = System.currentTimeMillis();
        loggingService.buildResLogger(request, responseWrapper, endTime - startTime);
        return responseWrapper;
    }
}
