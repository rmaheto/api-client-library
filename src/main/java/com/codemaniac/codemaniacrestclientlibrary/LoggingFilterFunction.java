package com.codemaniac.codemaniacrestclientlibrary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public class LoggingFilterFunction implements ExchangeFilterFunction {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilterFunction.class);
    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        long startTime = System.currentTimeMillis();
        logRequest(request);

        return next.exchange(request)
                .doOnNext(response -> logResponse(response, System.currentTimeMillis() - startTime))
                .doOnError(throwable -> log.error("Request Failed: {}", throwable.getMessage()));
    }

    private void logRequest(ClientRequest request) {
        log.info("Request: {} {}", request.method(), request.url());
        request.headers().forEach((name, values) -> values.forEach(value -> log.info("{}: {}", name, value)));
    }

    private void logResponse(ClientResponse response, long duration) {
        response.headers().asHttpHeaders()
                .forEach((name, values) -> values.forEach(value -> log.info("{}: {}", name, value)));

        response.bodyToMono(String.class).subscribe(body -> {
            log.info("Response Body: {}", body);

            if (body.isEmpty()) {
                log.warn("Blank response detected.");
                // Log as a security event
            }
        });

        log.info("Response Status Code: {}", response.statusCode());
        log.info("Request Duration: {} ms", duration);
    }
}
