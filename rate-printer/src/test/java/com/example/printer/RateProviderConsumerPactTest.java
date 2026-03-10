package com.example.printer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "currency-rate-provider")
class RateProviderConsumerPactTest {

    @Pact(consumer = "rate-printer")
    V4Pact usdRubRatePact(PactBuilder builder) {
        PactDslWithProvider pactDslBuilder = builder.usingLegacyDsl();
        return pactDslBuilder
                .given("USD/RUB rate is available")
                .uponReceiving("valid JSON-RPC getUsdRubRate request")
                .path("/rpc")
                .method("POST")
                .headers(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(new PactDslJsonBody()
                        .stringValue("jsonrpc", "2.0")
                        .stringValue("method", "getUsdRubRate")
                        .nullValue("params")
                        .integerType("id", 1))
                .willRespondWith()
                .status(200)
                .headers(Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .body(new PactDslJsonBody()
                        .stringValue("jsonrpc", "2.0")
                        .decimalType("result", 92.50)
                        .integerType("id", 1))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "usdRubRatePact")
    void shouldLoadRateFromProviderContract(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        JsonRpcRequest request = new JsonRpcRequest("2.0", "getUsdRubRate", null, 1);

        JsonRpcResponse response = restTemplate.postForObject(
                mockServer.getUrl() + "/rpc", request, JsonRpcResponse.class);

        assertNotNull(response);
        assertEquals("2.0", response.jsonrpc());
        assertEquals(1, response.id());
        assertNotNull(response.result());
    }
}
