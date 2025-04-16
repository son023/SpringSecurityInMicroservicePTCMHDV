package com.example.ftgokitchenservice;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {
    private Long id;
    private String itemName;
    private String notes;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}