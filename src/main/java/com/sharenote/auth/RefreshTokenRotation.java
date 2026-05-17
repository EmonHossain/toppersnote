package com.sharenote.auth;

import com.sharenote.user.User;

public record RefreshTokenRotation(
        User user,
        String refreshToken
) {
}
