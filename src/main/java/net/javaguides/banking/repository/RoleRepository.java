package net.javaguides.banking.repository;

import net.javaguides.banking.entity.AppRole;
import net.javaguides.banking.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(AppRole appRole);

}