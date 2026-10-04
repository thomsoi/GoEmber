package com.goember.hackathon.user;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final GuestAccess access;

    public UserController(UserService userService, GuestAccess access) {
        this.userService = userService;
        this.access = access;
    }

    @PostMapping
    public GuestUserResponse createUser(@RequestParam String name) {
        User user = userService.createUser(name);
        return new GuestUserResponse(user.getId(), user.getName(), user.getStreak(), user.getAccessToken());
    }

    @GetMapping
    public List<User> getUsers() {
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                "Guest directory is private");
    }

    @GetMapping("/{userId}")
    public User getUser(@PathVariable Long userId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        access.requireUser(userId, authorization);
        return userService.getUser(userId);
    }

    public record GuestUserResponse(Long id, String name, int streak, String accessToken) {}
}
