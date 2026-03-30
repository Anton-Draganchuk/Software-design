package com.example.printer;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                     ClientHttpLoggingInterceptor loggingInterceptor) {
        return builder
                .requestFactory(() -> loggingInterceptor.bufferingFactory(new SimpleClientHttpRequestFactory()))
                .additionalInterceptors(loggingInterceptor)
                .build();
    }
}
