package com.example.provider;

import java.nio.charset.StandardCharsets;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class ZooKeeperServiceRegistrar {

    private final String connectString;
    private final String basePath;
    private final String serviceName;
    private final String host;
    private final int port;
    private final String rpcPath;

    private CuratorFramework client;
    private String registeredNodePath;

    public ZooKeeperServiceRegistrar(
            @Value("${service-registry.connect-string}") String connectString,
            @Value("${service-registry.base-path}") String basePath,
            @Value("${service-registry.service-name}") String serviceName,
            @Value("${provider.host}") String host,
            @Value("${server.port}") int port,
            @Value("${provider.rpc-path}") String rpcPath) {
        this.connectString = connectString;
        this.basePath = basePath;
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
        this.rpcPath = rpcPath;
    }

    @PostConstruct
    public void register() {
        try {
            client = CuratorFrameworkFactory.newClient(connectString, new ExponentialBackoffRetry(1_000, 3));
            client.start();
            client.blockUntilConnected();

            String instancesPath = basePath + "/" + serviceName + "/instances";
            client.create().creatingParentsIfNeeded().forPath(instancesPath);

            String instanceUrl = "http://" + host + ":" + port + rpcPath;
            registeredNodePath = client.create()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(instancesPath + "/instance-", instanceUrl.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to register service in ZooKeeper", exception);
        }
    }

    @PreDestroy
    public void unregister() {
        if (client == null) {
            return;
        }

        try {
            if (registeredNodePath != null && client.checkExists().forPath(registeredNodePath) != null) {
                client.delete().forPath(registeredNodePath);
            }
        } catch (Exception ignored) {
        } finally {
            client.close();
        }
    }
}
