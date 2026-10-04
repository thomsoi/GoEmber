package com.goember.hackathon.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GuestAccess {
    private final UserRepository users;
    public GuestAccess(UserRepository users) { this.users = users; }

    public void requireUser(Long userId, String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() > 100) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A guest credential is required");
        }
        var user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Guest session has expired"));
        if (!MessageDigest.isEqual(user.getCredentialHash().getBytes(StandardCharsets.US_ASCII),
                hash(authorization.substring(7)).getBytes(StandardCharsets.US_ASCII))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This passport belongs to another guest");
        }
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
