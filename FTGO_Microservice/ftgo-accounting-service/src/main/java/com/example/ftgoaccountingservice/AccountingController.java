package com.example.ftgoaccountingservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounting")
public class AccountingController {

    @Autowired
    private AccountingService accountingService;

    @GetMapping("/by-order/{orderId}")
    @PreAuthorize("hasAuthority('RESTAURANT_READ') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAccountingDetailsByOrderId(@PathVariable Long orderId) {
        return accountingService.getAccountingDetailsByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}