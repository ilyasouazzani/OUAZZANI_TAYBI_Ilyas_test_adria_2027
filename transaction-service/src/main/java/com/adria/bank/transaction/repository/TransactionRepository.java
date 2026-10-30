package com.adria.bank.transaction.repository;

import com.adria.bank.transaction.domain.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<BankTransaction, UUID> {
    List<BankTransaction> findByAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(UUID accountId, UUID destinationAccountId);
    List<BankTransaction> findAllByOrderByCreatedAtDesc();
}
