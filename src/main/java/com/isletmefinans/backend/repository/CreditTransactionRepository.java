package com.isletmefinans.backend.repository;

import com.isletmefinans.backend.entity.CreditTransaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {

    @Override
    @EntityGraph(attributePaths = {"customer", "customer.transactions"})
    Optional<CreditTransaction> findById(Long id);
}
