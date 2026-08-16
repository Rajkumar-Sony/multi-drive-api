package com.multidrive.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleDriveFileMutationRequest(

		String name,

		String mimeType,

		List<String> parents,

		Boolean trashed) {
}