package com.isletmefinans.backend.repository;

import com.isletmefinans.backend.entity.CreditCustomer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditCustomerRepository extends JpaRepository<CreditCustomer, Long> {

    @Override
    @EntityGraph(attributePaths = "transactions")
    Optional<CreditCustomer> findById(Long id);

    @EntityGraph(attributePaths = "transactions")
    Optional<CreditCustomer> findByPhoneNumberIgnoreCase(String phoneNumber);

    List<CreditCustomer> findByArchivedFalseOrderByUpdatedAtDesc();

    List<CreditCustomer> findByArchivedTrueOrderByArchivedAtDesc();
}
