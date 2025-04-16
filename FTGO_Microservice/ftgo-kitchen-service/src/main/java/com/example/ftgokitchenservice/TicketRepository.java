package com.example.ftgokitchenservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByKitchenOrderId(Long kitchenOrderId);
    List<Ticket> findByStatus(String status);
}