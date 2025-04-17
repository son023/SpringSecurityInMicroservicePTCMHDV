package com.example.ftgoapigateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {


    @GetMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", "Dịch vụ xác thực tạm thời không khả dụng. Vui lòng thử lại sau.");
        response.put("service", "Auth Service");

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }


    @GetMapping("/orders")
    public Mono<ResponseEntity<Map<String, Object>>> orderServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", "Dịch vụ đơn hàng tạm thời không khả dụng. Vui lòng thử lại sau.");
        response.put("service", "Order Service");

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }


    @GetMapping("/kitchen")
    public Mono<ResponseEntity<Map<String, Object>>> kitchenServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", "Dịch vụ nhà bếp tạm thời không khả dụng. Vui lòng thử lại sau.");
        response.put("service", "Kitchen Service");

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }


    @GetMapping("/accounting")
    public Mono<ResponseEntity<Map<String, Object>>> accountingServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", "Dịch vụ kế toán tạm thời không khả dụng. Vui lòng thử lại sau.");
        response.put("service", "Accounting Service");

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }
}