package com.example.ftgoorderservice.controller;

import com.example.ftgoorderservice.dto.CreateOrderRequest;
import com.example.ftgoorderservice.service.OrderService;
import com.example.ftgoorderservice.dto.OrderDTO;
import com.example.ftgoorderservice.dto.OrderDetailsDTO;
import com.example.ftgoorderservice.entity.Order;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;


    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_READ') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        log.info("Nhận request lấy danh sách đơn hàng");
        List<Order> orders = orderService.getAllOrders();

        List<OrderDTO> orderDTOs = orders.stream()
                .map(order -> OrderDTO.builder()
                        .id(order.getId())
                        .customerId(order.getCustomerId())
                        .customerUsername(order.getCustomerUsername())
                        .kitchenId(order.getKitchenId())
                        .restaurantName(order.getRestaurantName())
                        .totalAmount(order.getTotalAmount())
                        .status(order.getStatus())
                        .createdAt(order.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        log.info("Trả về {} đơn hàng", orders.size());
        return ResponseEntity.ok(orderDTOs);
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_READ') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        log.info("Nhận request lấy thông tin đơn hàng: {}", id);
        try {
            Order order = orderService.getOrderById(id);

            OrderDTO orderDTO = OrderDTO.builder()
                    .id(order.getId())
                    .customerId(order.getCustomerId())
                    .customerUsername(order.getCustomerUsername())
                    .kitchenId(order.getKitchenId())
                    .restaurantName(order.getRestaurantName())
                    .deliveryAddress(order.getDeliveryAddress())
                    .deliveryTime(order.getDeliveryTime())
                    .totalAmount(order.getTotalAmount())
                    .status(order.getStatus())
                    .createdAt(order.getCreatedAt())
                    .build();

            log.info("Trả về thông tin đơn hàng: {}", id);
            return ResponseEntity.ok(orderDTO);
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin đơn hàng {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{orderId}/details")
    @PreAuthorize("hasAuthority('ORDER_READ') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_CHEF')")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) {
        log.info("Nhận request lấy chi tiết đơn hàng: {}", orderId);
        try {
            OrderDetailsDTO orderDetails = orderService.getOrderDetails(orderId);
            log.info("Trả về chi tiết đơn hàng: {}", orderId);
            return ResponseEntity.ok(orderDetails);
        } catch (AccessDeniedException e) {
            log.warn("Truy cập bị từ chối khi lấy chi tiết đơn hàng {}: {}", orderId, e.getMessage());
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết đơn hàng {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping
    @PreAuthorize("hasAuthority('ORDER_CREATE') or hasAuthority('ROLE_USER')")
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest createRequest) {
        log.info("Nhận request tạo đơn hàng mới");
        try {
            OrderDTO createdOrder = orderService.createOrder(createRequest);
            log.info("Đã tạo đơn hàng mới với ID: {}", createdOrder.getId());
            return ResponseEntity.ok(createdOrder);
        } catch (Exception e) {
            log.error("Lỗi khi tạo đơn hàng: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}