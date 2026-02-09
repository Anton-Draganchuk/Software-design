package com.example.printer;

public record JsonRpcResponse(String jsonrpc, Double result, Object id) {
}
