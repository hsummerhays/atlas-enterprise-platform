package io.github.hsummerhays.atlas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AtlasAgentPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtlasAgentPlatformApplication.class, args);
    }
}
