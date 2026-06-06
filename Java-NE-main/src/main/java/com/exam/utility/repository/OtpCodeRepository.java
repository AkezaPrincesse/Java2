package com.exam.utility.repository;

import com.exam.utility.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findTopByEmailAndPurposeAndUsedFalseOrderByGeneratedAtDesc(String email, String purpose);

    @Modifying
    @Query("UPDATE OtpCode o SET o.used = true WHERE o.email = :email AND o.purpose = :purpose")
    void invalidateAll(String email, String purpose);
}
