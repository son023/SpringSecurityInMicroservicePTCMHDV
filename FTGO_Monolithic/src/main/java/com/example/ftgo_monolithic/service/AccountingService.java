package com.example.ftgo_monolithic.service;

import com.example.ftgo_monolithic.model.AccountingDetails;
import com.example.ftgo_monolithic.repository.AccountingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccountingService {

    @Autowired
    private AccountingRepository accountingRepository;

    public Optional<AccountingDetails> getAccountingDetailsByOrderId(Long orderId) {
        return accountingRepository.findByOrderId(orderId);
    }
}