package com.springboot.courses.repository;

import com.springboot.courses.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsUserByEmail(String email);

    boolean existsUserByPhoneNumber(String phoneNumber);
}
