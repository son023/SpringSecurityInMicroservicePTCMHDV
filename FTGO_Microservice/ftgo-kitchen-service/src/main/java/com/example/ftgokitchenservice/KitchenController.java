package com.example.ftgokitchenservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/kitchen")
public class KitchenController {
    private static final Logger log = LoggerFactory.getLogger(KitchenController.class);

    private final KitchenService kitchenService;

    @Autowired
    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }


    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('KITCHEN_READ') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_CHEF')")
    public ResponseEntity<List<KitchenOrderDTO>> getAllKitchenOrders() {
        log.info("Nhận request lấy tất cả đơn nhà bếp");
        List<KitchenOrderDTO> kitchenOrders = kitchenService.getAllKitchenOrders();
        log.info("Trả về {} đơn nhà bếp", kitchenOrders.size());
        return ResponseEntity.ok(kitchenOrders);
    }


    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAuthority('KITCHEN_READ') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_CHEF')")
    public ResponseEntity<?> getKitchenOrderById(@PathVariable Long id) {
        log.info("Nhận request lấy đơn nhà bếp theo ID: {}", id);
        try {
            KitchenOrderDTO kitchenOrder = kitchenService.getKitchenOrderById(id);
            log.info("Trả về đơn nhà bếp với ID: {}", id);
            return ResponseEntity.ok(kitchenOrder);
        } catch (Exception e) {
            log.error("Lỗi khi lấy đơn nhà bếp ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/orders/by-order/{orderId}")
    @PreAuthorize("hasAuthority('KITCHEN_READ') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_CHEF')")
    public ResponseEntity<?> getKitchenOrderByOrderId(@PathVariable Long orderId) {
        log.info("Nhận request lấy đơn nhà bếp theo order ID: {}", orderId);
        try {
            KitchenOrderDTO kitchenOrder = kitchenService.getKitchenOrderByOrderId(orderId);
            log.info("Trả về đơn nhà bếp cho order ID: {}", orderId);
            return ResponseEntity.ok(kitchenOrder);
        } catch (Exception e) {
            log.error("Lỗi khi lấy đơn nhà bếp cho order ID {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('KITCHEN_CREATE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createKitchenOrder(@RequestBody KitchenOrderDTO kitchenOrderDTO) {
        log.info("Nhận request tạo đơn nhà bếp mới cho order ID: {}", kitchenOrderDTO.getOrderId());
        try {
            KitchenOrderDTO createdOrder = kitchenService.createKitchenOrder(kitchenOrderDTO);
            log.info("Đã tạo đơn nhà bếp mới với ID: {}", createdOrder.getId());
            return ResponseEntity.ok(createdOrder);
        } catch (Exception e) {
            log.error("Lỗi khi tạo đơn nhà bếp: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @PutMapping("/orders/{orderId}/status")
    @PreAuthorize("hasAuthority('KITCHEN_CREATE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_CHEF')")
    public ResponseEntity<?> updateKitchenOrderStatus(
            @PathVariable Long orderId,
            @RequestBody KitchenOrderStatusDTO statusDTO) {
        log.info("Nhận request cập nhật trạng thái đơn nhà bếp với order ID: {} thành {}", orderId, statusDTO.getStatus());
        try {
            KitchenOrderDTO updatedOrder = kitchenService.updateKitchenOrderStatus(orderId, statusDTO);
            log.info("Đã cập nhật trạng thái đơn nhà bếp cho order ID: {}", orderId);
            return ResponseEntity.ok(updatedOrder);
        } catch (AccessDeniedException e) {
            log.warn("Truy cập bị từ chối: {}", e.getMessage());
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật trạng thái đơn nhà bếp: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @PutMapping("/tickets/{ticketId}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateTicketStatus(
            @PathVariable Long ticketId,
            @RequestBody Map<String, String> statusMap) {
        String status = statusMap.get("status");
        log.info("Nhận request cập nhật trạng thái ticket ID: {} thành {}", ticketId, status);

        try {
            TicketDTO updatedTicket = kitchenService.updateTicketStatus(ticketId, status);
            log.info("Đã cập nhật trạng thái ticket ID: {}", ticketId);
            return ResponseEntity.ok(updatedTicket);
        } catch (AccessDeniedException e) {
            log.warn("Truy cập bị từ chối: {}", e.getMessage());
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật trạng thái ticket: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}