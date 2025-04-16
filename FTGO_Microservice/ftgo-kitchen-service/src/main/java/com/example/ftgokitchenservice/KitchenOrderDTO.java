package com.example.ftgokitchenservice;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KitchenOrderDTO {
    private Long id;
    private Long orderId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime preparedAt;
    private Long chefId;
    private String chefName;
    private List<TicketDTO> tickets;
}

