package com.hakyung.barleyssal_spring.domain.account;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(
            name = "jakarta.persistence.lock.timeout",
            value = "2000"
    ))
    @Query("SELECT a FROM Account a WHERE a.userId = :userId")
    Optional<Account> findByUserIdWithLock(@Param("userId") Long userId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(
            name = "jakarta.persistence.lock.timeout",
            value = "2000"
    ))
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);
}
