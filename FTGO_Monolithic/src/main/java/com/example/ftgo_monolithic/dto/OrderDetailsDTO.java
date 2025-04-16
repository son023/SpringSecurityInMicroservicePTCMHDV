package com.example.ftgo_monolithic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailsDTO {
    private Long orderId;
    private String customerUsername;
    private String restaurantName;
    private BigDecimal totalAmount;
    private String orderStatus;
    private LocalDateTime orderTime;
    private String paymentStatus;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime transactionTime;
}


