package com.example.ftgoorderservice.client;

import com.example.ftgoorderservice.dto.KitchenOrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "kitchen-service",
        url = "${ftgo.service.kitchen-url}",
        configuration = FeignClientConfig.class)
public interface KitchenClient {

    @GetMapping("/kitchen/orders/by-order/{orderId}")
    KitchenOrderDTO getKitchenOrderByOrderId(@PathVariable("orderId") Long orderId);

    @PostMapping("/kitchen/orders")
    KitchenOrderDTO createKitchenOrder(@RequestBody KitchenOrderDTO kitchenOrderDTO);
}