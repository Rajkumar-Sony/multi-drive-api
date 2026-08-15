package com.multidrive.api.service;

public interface GoogleTokenService {

    String getValidAccessToken(
            Long connectionId,
            Long userId
    );
}