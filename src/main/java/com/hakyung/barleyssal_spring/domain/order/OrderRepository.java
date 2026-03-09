package com.hakyung.barleyssal_spring.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o JOIN Account a ON o.accountId = a.id WHERE a.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByAccountIdAndUserId(@Param("accoutId") Long accountId, @Param("userId") Long userId);

    List<Order> findByAccountIdAndOrderStatus(Long accountId, OrderStatus orderStatus);
}
