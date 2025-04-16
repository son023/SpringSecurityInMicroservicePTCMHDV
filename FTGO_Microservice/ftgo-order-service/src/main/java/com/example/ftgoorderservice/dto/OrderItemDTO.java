package com.example.ftgoorderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long id;
    private Long menuItemId;
    private String name;
    private Integer quantity;
    private BigDecimal price;
    private String specialInstructions;
}