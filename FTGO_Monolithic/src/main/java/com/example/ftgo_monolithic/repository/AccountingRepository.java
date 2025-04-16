package com.example.ftgo_monolithic.repository;

import com.example.ftgo_monolithic.model.AccountingDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountingRepository extends JpaRepository<AccountingDetails, Long> {
    Optional<AccountingDetails> findByOrderId(Long orderId);
}