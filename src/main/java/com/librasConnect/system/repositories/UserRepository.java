package com.librasConnect.system.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.librasConnect.system.models.User;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "rules")
    Optional<User> findByEmail(String email);
}
