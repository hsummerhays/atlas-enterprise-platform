package com.inxpress.middleware.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.sns.endpoint-override:#{null}}")
    private String snsEndpointOverride;

    @Value("${aws.sqs.endpoint-override:#{null}}")
    private String sqsEndpointOverride;

    @Bean
    public SnsClient snsClient() {
        var builder = SnsClient.builder()
                .region(Region.of(region));
        if (snsEndpointOverride != null && !snsEndpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(snsEndpointOverride));
        }
        return builder.build();
    }

    @Bean
    public SqsClient sqsClient() {
        var builder = SqsClient.builder()
                .region(Region.of(region));
        if (sqsEndpointOverride != null && !sqsEndpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(sqsEndpointOverride));
        }
        return builder.build();
    }
}
