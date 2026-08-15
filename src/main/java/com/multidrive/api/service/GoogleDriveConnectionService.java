package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveAccountResponse;
import com.multidrive.api.dto.GoogleTokenResponse;
import com.multidrive.api.dto.GoogleUserInfoResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.User;

import java.util.List;

public interface GoogleDriveConnectionService {

    GoogleDriveConnection saveOrUpdateConnection(
            User user,
            GoogleUserInfoResponse googleUserInfo,
            GoogleTokenResponse tokenResponse
    );

    List<GoogleDriveAccountResponse> getConnectedAccounts(
            Long userId
    );
}