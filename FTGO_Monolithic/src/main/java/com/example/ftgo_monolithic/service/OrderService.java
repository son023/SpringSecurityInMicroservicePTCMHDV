package com.example.ftgo_monolithic.service;

import com.example.ftgo_monolithic.dto.OrderDetailsDTO;
import com.example.ftgo_monolithic.model.AccountingDetails;
import com.example.ftgo_monolithic.model.Order;
import com.example.ftgo_monolithic.model.Restaurant;
import com.example.ftgo_monolithic.model.User;
import com.example.ftgo_monolithic.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private AccountingService accountingService;

    @Autowired
    private UserService userService;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
    }

    public OrderDetailsDTO getOrderDetails(Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isRestaurantEmployee = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_RESTAURANT_EMPLOYEE"));

        Order order = getOrderById(orderId);

        User currentUser = userService.findByUsername(username);

        // Kiểm tra phân quyền
        if (!isAdmin) {
            if (isRestaurantEmployee) {
                // Nhân viên nhà hàng chỉ có thể xem đơn hàng của nhà hàng họ quản lý
                List<Restaurant> managedRestaurants = restaurantService.getRestaurantsByManagerId(currentUser.getId());
                boolean isOrderFromManagedRestaurant = managedRestaurants.stream()
                        .anyMatch(r -> r.getId().equals(order.getRestaurant().getId()));

                if (!isOrderFromManagedRestaurant) {
                    throw new AccessDeniedException("Bạn không có quyền truy cập chi tiết đơn hàng này");
                }
            } else {
                // Người dùng thông thường chỉ có thể xem đơn hàng của họ
                if (!order.getCustomer().getId().equals(currentUser.getId())) {
                    throw new AccessDeniedException("Bạn không có quyền truy cập chi tiết đơn hàng này");
                }
            }
        }

        // Lấy thông tin kế toán
        Optional<AccountingDetails> accountingDetails = accountingService.getAccountingDetailsByOrderId(orderId);

        return OrderDetailsDTO.builder()
                .orderId(order.getId())
                .customerUsername(order.getCustomer().getUsername())
                .restaurantName(order.getRestaurant().getName())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getStatus())
                .orderTime(order.getCreatedAt())
                .paymentStatus(accountingDetails.map(AccountingDetails::getPaymentStatus).orElse("N/A"))
                .paymentMethod(accountingDetails.map(AccountingDetails::getPaymentMethod).orElse("N/A"))
                .transactionId(accountingDetails.map(AccountingDetails::getTransactionId).orElse("N/A"))
                .transactionTime(accountingDetails.map(AccountingDetails::getTransactionTime).orElse(null))
                .build();
    }
}