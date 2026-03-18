package com.hakyung.barleyssal_spring.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o JOIN Account a ON o.accountId = a.id WHERE a.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByAccountIdAndUserId(@Param("accoutId") Long accountId, @Param("userId") Long userId);

    Optional<Order> findByIdAndAccountId(Long orderId, Long accountId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Order o SET o.orderStatus = :targetStatus, o.updatedAt = CURRENT_TIMESTAMP WHERE o.orderStatus IN :sourceStatuses")
    int bulkUpdateStatus(@Param("targetStatus") OrderStatus targetStatus,
                         @Param("sourceStatuses") Collection<OrderStatus> sourceStatuses);

    List<Order> findTop1000ByCreatedAtBefore(Instant threshold);

    List<Order> findTop1000ByIdGreaterThanAndCreatedAtBetweenOrderByIdAsc(Long lastId, Instant start, Instant end);
}
