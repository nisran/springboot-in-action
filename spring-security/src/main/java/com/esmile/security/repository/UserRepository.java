package com.esmile.security.repository;

import com.esmile.security.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @Description TODO
 * @Date 10/7/2023
 * @Created by nisran
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserName(String name);
}
