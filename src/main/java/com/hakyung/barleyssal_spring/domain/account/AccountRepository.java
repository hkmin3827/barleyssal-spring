package com.hakyung.barleyssal_spring.domain.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUserId(Long userId);

    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.holdings WHERE a.userId = :userId")
    Optional<Account> findByUserIdWithHoldings(@Param("userId") Long userId);
}
