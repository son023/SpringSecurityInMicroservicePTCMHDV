package com.example.ftgo_monolithic.repository;

import com.example.ftgo_monolithic.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByManagerId(Long managerId);
}