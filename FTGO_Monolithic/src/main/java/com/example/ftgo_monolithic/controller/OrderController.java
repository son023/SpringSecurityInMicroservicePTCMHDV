package com.example.ftgo_monolithic.controller;

import com.example.ftgo_monolithic.dto.OrderDetailsDTO;
import com.example.ftgo_monolithic.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    //spring security sẽ kiểm tra jsessionId và khôi phục security context trong filter SecurityContextPersistenceFilter
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) {
        try {
            OrderDetailsDTO orderDetails = orderService.getOrderDetails(orderId);
            return ResponseEntity.ok(orderDetails);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy thông tin đơn hàng: " + e.getMessage());
        }
    }
}