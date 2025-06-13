package net.javaguides.banking.repository;

import net.javaguides.banking.entity.Transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;


public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    public Page<Transaction> findByAccountIdOrderByTimestampDesc(Long accountId, Pageable pageable);
}
