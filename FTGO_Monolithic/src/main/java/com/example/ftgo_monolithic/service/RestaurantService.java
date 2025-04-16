package com.example.ftgo_monolithic.service;

import com.example.ftgo_monolithic.model.Restaurant;
import com.example.ftgo_monolithic.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantService {

    @Autowired
    private RestaurantRepository restaurantRepository;

    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    public Restaurant getRestaurantById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà hàng với ID: " + id));
    }

    public List<Restaurant> getRestaurantsByManagerId(Long managerId) {
        return restaurantRepository.findByManagerId(managerId);
    }
}