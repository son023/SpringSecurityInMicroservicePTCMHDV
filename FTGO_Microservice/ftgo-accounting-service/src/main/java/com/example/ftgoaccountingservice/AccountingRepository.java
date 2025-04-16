package com.example.ftgoaccountingservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountingRepository extends JpaRepository<AccountingDetails, Long> {
    Optional<AccountingDetails> findByOrderId(Long orderId);
}