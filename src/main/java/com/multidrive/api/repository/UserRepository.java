package com.multidrive.api.repository;

import com.multidrive.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGoogleSubjectId(String googleSubjectId);
}