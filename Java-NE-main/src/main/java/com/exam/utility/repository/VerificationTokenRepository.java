package com.exam.utility.repository;

import com.exam.utility.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUserIdAndUsedFalse(Long userId);

    @Modifying
    @Query("DELETE FROM VerificationToken v WHERE v.user.id = :userId")
    void deleteByUserId(Long userId);
}
