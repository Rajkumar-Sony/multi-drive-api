package com.multidrive.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleDriveFileMutationRequest(

		String name,

		String mimeType,

		List<String> parents,

		Boolean trashed,

		Map<String, String> appProperties) {

	public GoogleDriveFileMutationRequest(String name, String mimeType, List<String> parents, Boolean trashed) {

		this(name, mimeType, parents, trashed, null);
	}

}
