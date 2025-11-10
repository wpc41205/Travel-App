package com.techup.travel_app.repository;

import com.techup.travel_app.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    
    List<Trip> findByAuthorId(Long authorId);
    
    List<Trip> findByTitleContainingIgnoreCase(String title);
}

