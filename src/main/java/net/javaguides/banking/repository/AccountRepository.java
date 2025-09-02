package net.javaguides.banking.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import net.javaguides.banking.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")) // 設定 3 秒逾時
    Optional<Account> findByIdForUpdate(Long id);

}
