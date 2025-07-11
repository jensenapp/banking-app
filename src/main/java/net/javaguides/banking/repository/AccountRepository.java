package net.javaguides.banking.repository;

import jakarta.persistence.LockModeType;
import net.javaguides.banking.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id=:id")
    Optional<Account> findByIdForUpdate(Long id);

}
