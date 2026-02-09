package com.example.provider;

public record JsonRpcResponse(String jsonrpc, Object result, Object id) {
}
