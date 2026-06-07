package com.exam.utility.repository;

import com.exam.utility.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%',:keyword,'%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%',:keyword,'%')) OR " +
           "u.phoneNumber LIKE CONCAT('%',:keyword,'%')")
    Page<User> searchUsers(String keyword, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.email = :email")
    void incrementFailedAttempts(String email);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountNonLocked = true, u.lockTime = null WHERE u.email = :email")
    void resetFailedAttempts(String email);
}
