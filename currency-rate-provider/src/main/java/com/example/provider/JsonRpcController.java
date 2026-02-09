package com.example.provider;

import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class JsonRpcController {

    private final RateService rateService;

    public JsonRpcController(RateService rateService) {
        this.rateService = rateService;
    }

    @PostMapping("/rpc")
    public JsonRpcResponse getRate(@RequestBody JsonRpcRequest request) {
        if (request == null || !"2.0".equals(request.jsonrpc()) || !"getUsdRubRate".equals(request.method())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON-RPC request");
        }

        BigDecimal rate = rateService.currentUsdRubRate();
        return new JsonRpcResponse("2.0", rate, request.id());
    }
}
