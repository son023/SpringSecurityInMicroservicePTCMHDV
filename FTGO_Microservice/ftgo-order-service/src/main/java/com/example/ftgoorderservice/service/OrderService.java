package com.example.ftgoorderservice.service;

import com.example.ftgoorderservice.dto.AccountingDetailsDTO;
import com.example.ftgoorderservice.dto.CreateOrderRequest;
import com.example.ftgoorderservice.repository.OrderRepository;
import com.example.ftgoorderservice.client.AccountingClient;
import com.example.ftgoorderservice.client.KitchenClient;
import com.example.ftgoorderservice.dto.*;
import com.example.ftgoorderservice.entity.Order;
import com.example.ftgoorderservice.entity.OrderItem;
import com.example.ftgoorderservice.security.JwtUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final KitchenClient kitchenClient;
    private final AccountingClient accountingClient;
    private final JwtUtil jwtUtil;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        KitchenClient kitchenClient,
                        AccountingClient accountingClient,
                        JwtUtil jwtUtil) {
        this.orderRepository = orderRepository;
        this.kitchenClient = kitchenClient;
        this.accountingClient = accountingClient;
        this.jwtUtil = jwtUtil;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
    }

    public OrderDetailsDTO getOrderDetails(Long orderId) {
        log.info("Bắt đầu lấy thông tin chi tiết đơn hàng với ID: {}", orderId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isRead = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ORDER_READ"));

        Order order = getOrderById(orderId);
        log.debug("Đã lấy thông tin cơ bản của đơn hàng: {}", order.getId());


        if (!isAdmin && !isRead) {
            if (!order.getCustomerUsername().equals(username)) {
                log.warn("Truy cập bị từ chối: Người dùng {} không sở hữu đơn hàng của {}",
                        username, order.getCustomerUsername());
                throw new AccessDeniedException("Bạn không có quyền truy cập chi tiết đơn hàng này");
            }
        }

        KitchenOrderDTO kitchenOrder = getKitchenOrderDetails(orderId);
        log.debug("Đã lấy thông tin từ Kitchen Service cho đơn hàng: trạng thái = {}",
                kitchenOrder != null ? kitchenOrder.getStatus() : "N/A");


        AccountingDetailsDTO accountingDetails = getAccountingDetails(orderId);
        log.debug("Đã lấy thông tin kế toán cho đơn hàng: trạng thái = {}",
                accountingDetails != null ? accountingDetails.getPaymentStatus() : "N/A");

        OrderDetailsDTO orderDetails = OrderDetailsDTO.builder()
                .orderId(order.getId())
                .customerUsername(order.getCustomerUsername())
                .restaurantName(order.getRestaurantName())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getStatus())
                .orderTime(order.getCreatedAt())
                .paymentStatus(accountingDetails != null ? accountingDetails.getPaymentStatus() : "N/A")
                .paymentMethod(accountingDetails != null ? accountingDetails.getPaymentMethod() : "N/A")
                .transactionId(accountingDetails != null ? accountingDetails.getTransactionId() : "N/A")
                .transactionTime(accountingDetails != null ? accountingDetails.getTransactionTime() : null)
                .kitchenStatus(kitchenOrder != null ? kitchenOrder.getStatus() : "N/A")
                .chefName(kitchenOrder != null ? kitchenOrder.getChefName() : "N/A")
                .preparedAt(kitchenOrder != null ? kitchenOrder.getPreparedAt() : null)
                .build();

        log.info("Hoàn thành lấy thông tin chi tiết đơn hàng: {}", orderId);
        return orderDetails;
    }


    public Order createOrder(Order order) {
        log.info("Bắt đầu tạo đơn hàng mới");

        // Lưu đơn hàng vào database
        order.setStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        log.info("Đã lưu đơn hàng mới với ID: {}", savedOrder.getId());

        // Tạo đơn nhà bếp tương ứng
        try {
            KitchenOrderDTO kitchenOrderDTO = new KitchenOrderDTO();
            kitchenOrderDTO.setOrderId(savedOrder.getId());

            // Tạo danh sách ticket giả định
            List<TicketDTO> tickets = new ArrayList<>();
            // Trong thực tế, tickets sẽ được tạo dựa trên chi tiết đơn hàng
            TicketDTO ticketDTO = new TicketDTO();
            ticketDTO.setItemName("Món ăn từ " + savedOrder.getRestaurantName());
            tickets.add(ticketDTO);

            kitchenOrderDTO.setTickets(tickets);

            KitchenOrderDTO createdKitchenOrder = createKitchenOrder(kitchenOrderDTO);
            log.info("Đã tạo đơn nhà bếp cho đơn hàng ID: {}", savedOrder.getId());
        } catch (Exception e) {
            log.error("Lỗi khi tạo đơn nhà bếp: {}", e.getMessage());
            // Không rollback đơn hàng, chỉ cập nhật trạng thái
            savedOrder.setStatus("KITCHEN_SERVICE_FAILED");
            orderRepository.save(savedOrder);
        }

        return savedOrder;
    }


    @CircuitBreaker(name = "kitchenService", fallbackMethod = "getKitchenOrderDetailsFallback")
    @Retry(name = "kitchenService")
    private KitchenOrderDTO getKitchenOrderDetails(Long orderId) {
        log.info("Gọi Kitchen Service để lấy thông tin đơn nhà bếp cho order ID: {}", orderId);
        try {
            KitchenOrderDTO kitchenOrder = kitchenClient.getKitchenOrderByOrderId(orderId);
            log.info("Đã nhận thông tin từ Kitchen Service cho đơn hàng {}: trạng thái = {}",
                    orderId, kitchenOrder.getStatus());
            return kitchenOrder;
        } catch (Exception e) {
            log.error("Lỗi khi gọi Kitchen Service: {}", e.getMessage());
            throw e;
        }
    }

    private KitchenOrderDTO getKitchenOrderDetailsFallback(Long orderId, Exception ex) {
        log.error("Fallback cho Kitchen Service được kích hoạt. Order ID: {}, Lỗi: {}",
                orderId, ex.getMessage());
        return null;
    }


    @CircuitBreaker(name = "kitchenService", fallbackMethod = "createKitchenOrderFallback")
    @Retry(name = "kitchenService")
    private KitchenOrderDTO createKitchenOrder(KitchenOrderDTO kitchenOrderDTO) {
        log.info("Gọi Kitchen Service để tạo đơn nhà bếp mới cho order ID: {}", kitchenOrderDTO.getOrderId());
        try {
            KitchenOrderDTO createdOrder = kitchenClient.createKitchenOrder(kitchenOrderDTO);
            log.info("Đã tạo đơn nhà bếp thành công với ID: {}", createdOrder.getId());
            return createdOrder;
        } catch (Exception e) {
            log.error("Lỗi khi gọi Kitchen Service để tạo đơn nhà bếp: {}", e.getMessage());
            throw e;
        }
    }


    private KitchenOrderDTO createKitchenOrderFallback(KitchenOrderDTO kitchenOrderDTO, Exception ex) {
        log.error("Fallback cho Kitchen Service (tạo đơn) được kích hoạt. Order ID: {}, Lỗi: {}",
                kitchenOrderDTO.getOrderId(), ex.getMessage());
        throw new RuntimeException("Không thể tạo đơn nhà bếp: " + ex.getMessage());
    }

    @CircuitBreaker(name = "accountingService", fallbackMethod = "getAccountingDetailsFallback")
    @Retry(name = "accountingService")
    private AccountingDetailsDTO getAccountingDetails(Long orderId) {
        log.info("Gọi Accounting Service để lấy thông tin thanh toán cho đơn hàng: {}", orderId);
        try {
            AccountingDetailsDTO details = accountingClient.getAccountingDetailsByOrderId(orderId);
            log.info("Đã nhận thông tin từ Accounting Service cho đơn hàng {}: trạng thái = {}",
                    orderId, details.getPaymentStatus());
            return details;
        } catch (Exception e) {
            log.error("Lỗi khi gọi Accounting Service: {}", e.getMessage());
            throw e;
        }
    }

    private AccountingDetailsDTO getAccountingDetailsFallback(Long orderId, Exception ex) {
        log.error("Fallback cho Accounting Service được kích hoạt. Order ID: {}, Lỗi: {}",
                orderId, ex.getMessage());
        return null;
    }

    public OrderDTO createOrder(CreateOrderRequest createRequest) {
        log.info("Bắt đầu tạo đơn hàng mới cho nhà hàng: {}", createRequest.getRestaurantName());

        // Lấy thông tin người dùng hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // Lấy JWT token từ request
        String token = extractJwtToken();
        Long customerId = null;

        if (token != null) {
            try {
                // Lấy userId từ token - giả sử nó được lưu trong claim "sub"
                String sub = jwtUtil.extractClaim(token, claims -> claims.get("sub", String.class));
                if (sub != null) {
                    try {
                        customerId = Long.parseLong(sub);
                    } catch (NumberFormatException e) {
                        log.warn("Không thể parse userId từ JWT token: {}", sub);
                        customerId = 0L; // Giá trị mặc định
                    }
                } else {
                    customerId = 0L; // Giá trị mặc định nếu claim sub không tồn tại
                }
            } catch (Exception e) {
                log.warn("Lỗi khi trích xuất thông tin người dùng từ JWT token: {}", e.getMessage());
                customerId = 0L; // Giá trị mặc định
            }
        } else {
            log.warn("Không tìm thấy JWT token trong request");
            customerId = 0L; // Giá trị mặc định
        }

        // Tạo đơn hàng mới
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setCustomerUsername(username);
        order.setRestaurantName(createRequest.getRestaurantName());
        order.setDeliveryAddress(createRequest.getDeliveryAddress());
        order.setStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now());

        // Thêm các mặt hàng
        List<OrderItem> orderItems = createRequest.getItems().stream()
                .map(itemRequest -> {
                    OrderItem item = new OrderItem();
                    item.setMenuItemId(itemRequest.getMenuItemId());
                    item.setName(itemRequest.getName());
                    item.setQuantity(itemRequest.getQuantity());
                    item.setPrice(itemRequest.getPrice());
                    item.setSpecialInstructions(itemRequest.getSpecialInstructions());
                    return item;
                })
                .collect(Collectors.toList());

        order.setItems(orderItems);

        // Tính tổng tiền
        BigDecimal totalAmount = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);

        // Lưu đơn hàng vào database
        Order savedOrder = orderRepository.save(order);
        log.info("Đã lưu đơn hàng mới với ID: {}", savedOrder.getId());

        // Tạo yêu cầu trong Kitchen Service
        try {
            // Tạo danh sách ticket cho Kitchen Service
            List<TicketDTO> tickets = orderItems.stream()
                    .map(item -> {
                        TicketDTO ticket = new TicketDTO();
                        ticket.setItemName(item.getName());
                        ticket.setNotes(item.getSpecialInstructions());
                        return ticket;
                    })
                    .collect(Collectors.toList());

            // Tạo đơn nhà bếp
            KitchenOrderDTO kitchenOrderDTO = new KitchenOrderDTO();
            kitchenOrderDTO.setOrderId(savedOrder.getId());
            kitchenOrderDTO.setTickets(tickets);

            KitchenOrderDTO createdKitchenOrder = createKitchenOrder(kitchenOrderDTO);
            log.info("Đã tạo đơn nhà bếp với ID: {} cho đơn hàng: {}",
                    createdKitchenOrder.getId(), savedOrder.getId());

            // Cập nhật kitchenId cho đơn hàng
            savedOrder.setKitchenId(createdKitchenOrder.getId());
            savedOrder = orderRepository.save(savedOrder);

        } catch (Exception e) {
            log.error("Lỗi khi tạo đơn nhà bếp: {}", e.getMessage());
            // Cập nhật trạng thái đơn hàng
            savedOrder.setStatus("KITCHEN_SERVICE_FAILED");
            savedOrder = orderRepository.save(savedOrder);
        }

        // Chuyển đổi thành DTO để trả về
        return convertToDTO(savedOrder);
    }

    private String extractJwtToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return authHeader.substring(7);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi trích xuất JWT token từ request: {}", e.getMessage());
        }
        return null;
    }

    private OrderDTO convertToDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> OrderItemDTO.builder()
                        .id(item.getId())
                        .menuItemId(item.getMenuItemId())
                        .name(item.getName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .specialInstructions(item.getSpecialInstructions())
                        .build())
                .collect(Collectors.toList());

        return OrderDTO.builder()
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
                .items(itemDTOs)
                .build();
    }

    public boolean isOrderByUserId(String token) {
        return orderRepository.existsById(jwtUtil.getUserId(token));
    }
}