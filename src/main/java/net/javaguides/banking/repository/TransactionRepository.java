package net.javaguides.banking.repository;

import net.javaguides.banking.entity.Transaction;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    public List<Transaction> findByAccountIdOrderByTimestampDesc(Long accountId);
}
