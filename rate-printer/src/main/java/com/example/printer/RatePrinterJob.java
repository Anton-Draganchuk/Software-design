package com.example.printer;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RatePrinterJob {

    private static final Logger log = LoggerFactory.getLogger(RatePrinterJob.class);

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
                log.info("{} USDRUB={} from {}", LocalDateTime.now(), response.result(), providerUrl);
            } else {
                log.warn("{} Failed to get rate from {}", LocalDateTime.now(), providerUrl);
            }
        } catch (Exception exception) {
            log.error("{} No available provider instances", LocalDateTime.now(), exception);
        }
    }
}
