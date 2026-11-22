package com.omar.beautycenter.user;

import java.util.List;
import java.util.Optional;
import com.omar.beautycenter.role.Role;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.username = :username")
    Optional<User> findByUsernameForUpdate(String username);

    boolean existsByUsername(String username);

    boolean existsByEmployeeId(Long employeeId);

    Optional<User> findByEmployeeId(Long employeeId);

    long countByActiveTrueAndRolesContaining(Role role);

    List<User> findByRegistrationStatusOrderByCreatedAtAsc(RegistrationStatus registrationStatus);

    List<User> findAllByOrderByUsernameAsc();
}
