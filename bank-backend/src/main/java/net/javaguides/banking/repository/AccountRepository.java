package net.javaguides.banking.repository;

import jakarta.persistence.LockModeType;
import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id=:id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Query("select a from Account  a where a.user.userId=:userId")
    List<Account> findAllAccountsByUserId(@Param("userId") Long userId);


    Long countByUser_UserId(Long userId);

    boolean existsByIdAndUser_UserId(Long accountId, Long userId);

}
