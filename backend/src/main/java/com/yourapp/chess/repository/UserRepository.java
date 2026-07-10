package com.yourapp.chess.repository;

import com.yourapp.chess.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // Spring Data parses this method name and generates the SQL - no implementation needed
    Optional<User> findByUsername(String username);
}