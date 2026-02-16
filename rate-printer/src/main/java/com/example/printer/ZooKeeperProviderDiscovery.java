package com.example.printer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ZooKeeperProviderDiscovery {

    private final String connectString;
    private final String basePath;
    private final String serviceName;

    private final AtomicReference<List<String>> providerUrls = new AtomicReference<>(List.of());
    private final AtomicInteger roundRobinCounter = new AtomicInteger();

    private CuratorFramework client;
    private CuratorCache cache;

    public ZooKeeperProviderDiscovery(
            @Value("${service-registry.connect-string}") String connectString,
            @Value("${service-registry.base-path}") String basePath,
            @Value("${service-registry.service-name}") String serviceName) {
        this.connectString = connectString;
        this.basePath = basePath;
        this.serviceName = serviceName;
    }

    @PostConstruct
    public void start() {
        try {
            client = CuratorFrameworkFactory.newClient(connectString, new ExponentialBackoffRetry(1_000, 3));
            client.start();
            client.blockUntilConnected();

            String instancesPath = instancesPath();
            if (client.checkExists().forPath(instancesPath) == null) {
                client.create().creatingParentsIfNeeded().forPath(instancesPath);
            }

            refreshProviderUrls();

            cache = CuratorCache.build(client, instancesPath);
            cache.listenable().addListener((type, oldData, data) -> refreshProviderUrls());
            cache.start();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize ZooKeeper discovery", exception);
        }
    }

    public String nextProviderUrl() {
        List<String> urls = providerUrls.get();
        if (urls.isEmpty()) {
            throw new IllegalStateException("No provider instances registered in ZooKeeper");
        }

        int index = Math.floorMod(roundRobinCounter.getAndIncrement(), urls.size());
        return urls.get(index);
    }

    @PreDestroy
    public void stop() {
        if (cache != null) {
            cache.close();
        }
        if (client != null) {
            client.close();
        }
    }

    private void refreshProviderUrls() {
        try {
            List<String> children = client.getChildren().forPath(instancesPath());
            List<String> discovered = new ArrayList<>(children.size());

            for (String child : children) {
                byte[] data = client.getData().forPath(instancesPath() + "/" + child);
                discovered.add(new String(data, StandardCharsets.UTF_8));
            }

            Collections.sort(discovered);
            providerUrls.set(List.copyOf(discovered));
        } catch (Exception exception) {
            providerUrls.set(List.of());
        }
    }

    private String instancesPath() {
        return basePath + "/" + serviceName + "/instances";
    }
}
