package com.multidrive.api.service.impl;

import com.multidrive.api.entity.User;
import com.multidrive.api.repository.UserRepository;
import com.multidrive.api.service.UserService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public User saveOrUpdateGoogleUser(
            String googleSubjectId,
            String email,
            String name,
            String pictureUrl
    ) {

        User user = userRepository
                .findByGoogleSubjectId(googleSubjectId)
                .orElseGet(User::new);

        user.setGoogleSubjectId(googleSubjectId);
        user.setEmail(email);
        user.setName(name);
        user.setPictureUrl(pictureUrl);

        return userRepository.save(user);
    }
}