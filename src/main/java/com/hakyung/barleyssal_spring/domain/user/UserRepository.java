package com.hakyung.barleyssal_spring.domain.user;

import com.hakyung.barleyssal_spring.application.user.dto.UsersListResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByIdAndDeletedAtIsNull(Long userId);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    String findUserNameById(Long id);

    @Query("""
    select 
            u.id as id,
            u.email as email,
            u.userName as userName,
            u.active as active
        from User u
        where u.role = com.hakyung.barleyssal_spring.domain.user.Role.ROLE_USER
        order by u.createdAt asc
    """)
    Page<UsersListResponse> findUsersByActive(@Param("active") boolean active, Pageable pageable);
}
