package com.multidrive.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleRefreshTokenResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        Long expiresIn,

        @JsonProperty("scope")
        String scope,

        @JsonProperty("token_type")
        String tokenType
) {
}