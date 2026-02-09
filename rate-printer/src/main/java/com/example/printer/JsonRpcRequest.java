package com.example.printer;

public record JsonRpcRequest(String jsonrpc, String method, Object params, Object id) {
}
