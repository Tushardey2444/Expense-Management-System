package com.manage_expense.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        SecurityScheme accessTokenScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name(AppConstants.ACCESS_TOKEN)
                .description("JWT access token stored in HttpOnly cookie");

        SecurityScheme refreshTokenScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name(AppConstants.REFRESH_TOKEN)
                .description("Refresh token stored in HttpOnly cookie");

        return new OpenAPI()
                .info(new Info()
                        .title("Expense Management System APIs")
                        .version("1.0")
                        .description("API documentation for Expense Management System"))
                .components(new Components()
                        .addSecuritySchemes("accessToken", accessTokenScheme)
                        .addSecuritySchemes("refreshToken", refreshTokenScheme))
                // set the access token global security requirement for all endpoints
                .addSecurityItem(new SecurityRequirement()
                        .addList("accessToken"));
    }
}
