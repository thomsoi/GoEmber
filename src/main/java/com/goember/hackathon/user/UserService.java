package com.goember.hackathon.user;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goember.hackathon.passport.Passport;
import com.goember.hackathon.passport.PassportRepository;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PassportRepository passportRepository;

	public UserService(UserRepository userRepository, PassportRepository passportRepository) {
		this.userRepository = userRepository;
		this.passportRepository = passportRepository;
	}

	@Transactional
	public User createUser(String name) {
		User user = userRepository.save(new User(name));
		passportRepository.save(new Passport(user));
		return user;
	}

	public List<User> getUsers() {
		return userRepository.findAll();
	}

	public User getUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
	}
}
