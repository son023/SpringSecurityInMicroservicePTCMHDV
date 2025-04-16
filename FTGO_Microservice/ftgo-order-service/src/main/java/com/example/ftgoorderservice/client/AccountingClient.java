package com.example.ftgoorderservice.client;

import com.example.ftgoorderservice.dto.AccountingDetailsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "accounting-service",
        url = "${ftgo.service.accounting-url}",
        configuration = FeignClientConfig.class)
public interface AccountingClient {

    @GetMapping("/accounting/by-order/{orderId}")
    AccountingDetailsDTO getAccountingDetailsByOrderId(@PathVariable("orderId") Long orderId);
}