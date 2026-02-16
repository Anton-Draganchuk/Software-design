package com.example.printer;

import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RatePrinterJob {

    private final RestTemplate restTemplate;
    private final ZooKeeperProviderDiscovery providerDiscovery;

    public RatePrinterJob(RestTemplate restTemplate,
                          ZooKeeperProviderDiscovery providerDiscovery) {
        this.restTemplate = restTemplate;
        this.providerDiscovery = providerDiscovery;
    }

    @Scheduled(fixedDelay = 5000)
    public void printRate() {
        try {
            String providerUrl = providerDiscovery.nextProviderUrl();
            JsonRpcRequest request = new JsonRpcRequest("2.0", "getUsdRubRate", null, 1);
            JsonRpcResponse response = restTemplate.postForObject(providerUrl, request, JsonRpcResponse.class);

            if (response != null && response.result() != null) {
                System.out.println(LocalDateTime.now() + " USDRUB=" + response.result() + " from " + providerUrl);
            } else {
                System.out.println(LocalDateTime.now() + " Failed to get rate from " + providerUrl);
            }
        } catch (Exception exception) {
            System.out.println(LocalDateTime.now() + " No available provider instances");
        }
    }
}
