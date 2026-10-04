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
		if (name == null || name.isBlank() || name.length() > 100) {
			throw new IllegalArgumentException("Name must contain between 1 and 100 characters");
		}
		User user = userRepository.save(new User(name.trim()));
		passportRepository.save(new Passport(user));
		return user;
	}

	public List<User> getUsers() {
		return userRepository.findAll();
	}

	public User getUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found: " + userId));
	}
}
