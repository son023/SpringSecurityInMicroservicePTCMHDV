package com.example.ftgoorderservice.dto;


import lombok.Data;

@Data
public class RestaurantDTO {
    private Long id;
    private String name;
    private String address;
    private Long managerId;
}