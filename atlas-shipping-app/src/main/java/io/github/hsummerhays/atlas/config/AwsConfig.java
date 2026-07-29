package io.github.hsummerhays.atlas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.sns.endpoint-override:#{null}}")
    private String snsEndpointOverride;

    @Bean
    public SnsClient snsClient() {
        var builder = SnsClient.builder()
                .region(Region.of(region));
        if (snsEndpointOverride != null && !snsEndpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(snsEndpointOverride));
        }
        return builder.build();
    }
}
