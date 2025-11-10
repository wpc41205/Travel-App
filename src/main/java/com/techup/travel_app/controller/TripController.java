package com.techup.travel_app.controller;

import com.techup.travel_app.dto.TripRequest;
import com.techup.travel_app.dto.TripResponse;
import com.techup.travel_app.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class TripController {
    
    private final TripService tripService;
    
    @PostMapping("/trips")
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody TripRequest request) {
        TripResponse response = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/trips/{id}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable Long id) {
        TripResponse response = tripService.getTripById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/trips")
    public ResponseEntity<List<TripResponse>> getAllTrips() {
        List<TripResponse> responses = tripService.getAllTrips();
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/trips/author/{authorId}")
    public ResponseEntity<List<TripResponse>> getTripsByAuthor(@PathVariable Long authorId) {
        List<TripResponse> responses = tripService.getTripsByAuthorId(authorId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/trips/search")
    public ResponseEntity<List<TripResponse>> searchTrips(@RequestParam String title) {
        List<TripResponse> responses = tripService.searchTripsByTitle(title);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/trips/{id}")
    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable Long id,
            @Valid @RequestBody TripRequest request) {
        TripResponse response = tripService.updateTrip(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/trips/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }
}

