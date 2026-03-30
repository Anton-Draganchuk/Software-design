package com.example.printer;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

@Component
public class ApplicationVersionLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ApplicationVersionLogger.class);

    private final Optional<BuildProperties> buildProperties;

    public ApplicationVersionLogger(Optional<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String version = buildProperties.map(BuildProperties::getVersion).orElse("unknown");
        String name = buildProperties.map(BuildProperties::getName).orElse("rate-printer");
        log.info("Starting application {} version {}", name, version);
    }
}
