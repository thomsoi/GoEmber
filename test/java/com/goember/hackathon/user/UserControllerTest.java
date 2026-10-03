package com.goember.hackathon.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService);
    }

    @Test
    void createUser_returnsSavedUser() {
        User savedUser = new User("Alice");
        when(userService.createUser("Alice")).thenReturn(savedUser);

        User created = userController.createUser("Alice");

        assertNotNull(created);
        assertEquals("Alice", created.getName());
    }

    @Test
    void getUsers_returnsAllUsers() {
        when(userService.getUsers()).thenReturn(List.of(new User("Alice"), new User("Bob")));

        List<User> users = userController.getUsers();

        assertEquals(2, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Bob", users.get(1).getName());
    }

    @Test
    void getUser_returnsUserById() {
        User expected = new User("Charlie");
        when(userService.getUser(7L)).thenReturn(expected);

        User actual = userController.getUser(7L);

        assertNotNull(actual);
        assertEquals("Charlie", actual.getName());
    }
}
