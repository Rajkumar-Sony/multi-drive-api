package com.multidrive.api.dto;

public record CurrentUserResponse(Long id, String email, String name, String pictureUrl) {
}