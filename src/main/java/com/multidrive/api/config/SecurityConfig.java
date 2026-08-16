package com.multidrive.api.config;

import com.multidrive.api.security.OAuth2LoginSuccessHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler
            oauth2LoginSuccessHandler;

    public SecurityConfig(
            OAuth2LoginSuccessHandler
                    oauth2LoginSuccessHandler
    ) {

        this.oauth2LoginSuccessHandler =
                oauth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(
                        csrf -> csrf
                                .ignoringRequestMatchers(
                                        "/api/google/webhooks/drive"
                                )
                )

                .authorizeHttpRequests(
                        auth -> auth

                                .requestMatchers(
                                        "/",
                                        "/error"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/google/webhooks/drive"
                                )
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                )

                .oauth2Login(
                        oauth2 -> oauth2
                                .successHandler(
                                        oauth2LoginSuccessHandler
                                )
                );

        return http.build();
    }
}