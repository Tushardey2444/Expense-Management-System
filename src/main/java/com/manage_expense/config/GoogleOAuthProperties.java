package com.manage_expense.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "google")
public class GoogleOAuthProperties {
    private String authUri;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
}
