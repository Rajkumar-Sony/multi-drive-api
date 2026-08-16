package com.multidrive.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleStartPageTokenResponse(

		@JsonProperty("startPageToken") String startPageToken,

		@JsonProperty("kind") String kind) {
}