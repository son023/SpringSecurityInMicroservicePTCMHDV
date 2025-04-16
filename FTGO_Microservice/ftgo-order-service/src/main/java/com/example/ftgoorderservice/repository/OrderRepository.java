package com.example.ftgoorderservice.repository;

import com.example.ftgoorderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
    List<Order> findByCustomerUsername(String customerUsername);
    List<Order> findByKitchenId(Long kitchenId);
    List<Order> findByStatus(String status);
    boolean existsById(Long aLong);
}