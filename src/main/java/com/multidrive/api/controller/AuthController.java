package com.multidrive.api.controller;

import com.multidrive.api.dto.CurrentUserResponse;
import com.multidrive.api.entity.User;
import com.multidrive.api.service.UserService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(
            @AuthenticationPrincipal OidcUser oidcUser
    ) {

        User user = userService.findByGoogleSubjectId(
                oidcUser.getSubject()
        );

        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPictureUrl()
        );
    }
}