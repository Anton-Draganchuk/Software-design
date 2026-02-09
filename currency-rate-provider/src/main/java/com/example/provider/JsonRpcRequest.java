package com.example.provider;

public record JsonRpcRequest(String jsonrpc, String method, Object params, Object id) {
}
