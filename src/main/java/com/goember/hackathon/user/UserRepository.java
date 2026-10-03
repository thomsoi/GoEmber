package com.goember.hackathon.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

/*
We have access to:

userRepository.findById(1L);

userRepository.findAll();

userRepository.save(user);

userRepository.delete(user);

userRepository.count();
*/
