package com.multidrive.api.service;

import com.multidrive.api.entity.User;

public interface UserService {

    User saveOrUpdateGoogleUser(
            String googleSubjectId,
            String email,
            String name,
            String pictureUrl
    );

    User findByGoogleSubjectId(String googleSubjectId);
}