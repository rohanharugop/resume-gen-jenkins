
package com.resume.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for defining shared application beans like RestTemplate and ObjectMapper.
 */
@Configuration
public  class GroqConfig {

    /**
     * Creates and configures a RestTemplate bean for making HTTP requests.
     *
     * @return a new RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Creates and configures an ObjectMapper bean for JSON processing.
     *
     * @return a new ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
