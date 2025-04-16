package com.example.ftgoorderservice.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountingDetailsDTO {
    private Long id;
    private Long orderId;
    private String paymentStatus;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime transactionTime;
}