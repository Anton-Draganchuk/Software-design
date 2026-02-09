package com.example.printer;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RatePrinterJob {

    private final RestTemplate restTemplate;
    private final String providerUrl;

    public RatePrinterJob(RestTemplate restTemplate,
                          @Value("${provider.url}") String providerUrl) {
        this.restTemplate = restTemplate;
        this.providerUrl = providerUrl;
    }

    @Scheduled(fixedDelay = 5000)
    public void printRate() {
        JsonRpcRequest request = new JsonRpcRequest("2.0", "getUsdRubRate", null, 1);
        JsonRpcResponse response = restTemplate.postForObject(providerUrl, request, JsonRpcResponse.class);

        if (response != null && response.result() != null) {
            System.out.println(LocalDateTime.now() + " USDRUB=" + response.result());
        } else {
            System.out.println(LocalDateTime.now() + " Failed to get rate");
        }
    }
}
