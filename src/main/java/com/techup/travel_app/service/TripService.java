package com.techup.travel_app.service;

import com.techup.travel_app.dto.TripRequest;
import com.techup.travel_app.dto.TripResponse;
import com.techup.travel_app.entity.Trip;
import com.techup.travel_app.entity.User;
import com.techup.travel_app.repository.TripRepository;
import com.techup.travel_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {
    
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public TripResponse createTrip(TripRequest request) {
        // Verify author exists
        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getAuthorId()));
        
        Trip trip = new Trip();
        trip.setTitle(request.getTitle());
        trip.setDescription(request.getDescription());
        trip.setPhotos(request.getPhotos() != null ? request.getPhotos() : new ArrayList<>());
        trip.setTags(request.getTags() != null ? request.getTags() : new ArrayList<>());
        trip.setLatitude(request.getLatitude());
        trip.setLongitude(request.getLongitude());
        trip.setAuthorId(request.getAuthorId());
        
        Trip savedTrip = tripRepository.save(trip);
        return mapToResponse(savedTrip);
    }
    
    public TripResponse getTripById(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));
        return mapToResponse(trip);
    }
    
    public List<TripResponse> getAllTrips() {
        return tripRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<TripResponse> getTripsByAuthorId(Long authorId) {
        return tripRepository.findByAuthorId(authorId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<TripResponse> searchTripsByTitle(String title) {
        return tripRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public TripResponse updateTrip(Long id, TripRequest request) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));
        
        // Verify author exists if authorId is being changed
        if (request.getAuthorId() != null && !trip.getAuthorId().equals(request.getAuthorId())) {
            userRepository.findById(request.getAuthorId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getAuthorId()));
        }
        
        trip.setTitle(request.getTitle());
        trip.setDescription(request.getDescription());
        if (request.getPhotos() != null) {
            trip.setPhotos(request.getPhotos());
        }
        if (request.getTags() != null) {
            trip.setTags(request.getTags());
        }
        trip.setLatitude(request.getLatitude());
        trip.setLongitude(request.getLongitude());
        if (request.getAuthorId() != null) {
            trip.setAuthorId(request.getAuthorId());
        }
        
        Trip updatedTrip = tripRepository.save(trip);
        return mapToResponse(updatedTrip);
    }
    
    @Transactional
    public void deleteTrip(Long id) {
        if (!tripRepository.existsById(id)) {
            throw new RuntimeException("Trip not found with id: " + id);
        }
        tripRepository.deleteById(id);
    }
    
    private TripResponse mapToResponse(Trip trip) {
        TripResponse.TripResponseBuilder builder = TripResponse.builder()
                .id(trip.getId())
                .title(trip.getTitle())
                .description(trip.getDescription())
                .photos(trip.getPhotos() != null ? trip.getPhotos() : new ArrayList<>())
                .tags(trip.getTags() != null ? trip.getTags() : new ArrayList<>())
                .latitude(trip.getLatitude())
                .longitude(trip.getLongitude())
                .authorId(trip.getAuthorId())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt());
        
        // Load author info if available
        if (trip.getAuthor() != null) {
            builder.authorEmail(trip.getAuthor().getEmail())
                   .authorDisplayName(trip.getAuthor().getDisplayName());
        } else if (trip.getAuthorId() != null) {
            userRepository.findById(trip.getAuthorId()).ifPresent(author -> {
                builder.authorEmail(author.getEmail())
                       .authorDisplayName(author.getDisplayName());
            });
        }
        
        return builder.build();
    }
}

