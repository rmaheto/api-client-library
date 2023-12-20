package com.codemaniac.codemaniacrestclientlibrary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
@Configuration
public class RestClientConfig {

    private final LoggingRequestInterceptor loggingRequestInterceptor;

    @Autowired
    public RestClientConfig(LoggingRequestInterceptor loggingRequestInterceptor) {
        this.loggingRequestInterceptor = loggingRequestInterceptor;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(loggingRequestInterceptor);
        restTemplate.setInterceptors(interceptors);

        return restTemplate;
    }
}
