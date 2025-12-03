package com.techup.travel_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techup.travel_app.dto.TripRequest;
import com.techup.travel_app.dto.TripResponse;
import com.techup.travel_app.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Slf4j
public class TripController {
    
    private final TripService tripService;
    
    @PostMapping(value = "/trips", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody TripRequest request) {
        TripResponse response = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/trips", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TripResponse> createTripWithUploads(
            @RequestPart(value = "trip", required = false) String tripJson,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestPart(value = "primaryImage", required = false) MultipartFile primaryImage,
            @RequestPart(value = "additionalImages", required = false) List<MultipartFile> additionalImages,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        try {
            TripRequest tripRequest = new TripRequest();
            
            // Try to parse JSON first
            if (tripJson != null && !tripJson.trim().isEmpty()) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    tripRequest = objectMapper.readValue(tripJson, TripRequest.class);
                } catch (Exception e) {
                    log.warn("Failed to parse trip JSON, using form params instead: {}", e.getMessage());
                }
            }
            
            // Override with form params if provided (form params take precedence)
            if (title != null && !title.trim().isEmpty()) {
                tripRequest.setTitle(title);
            }
            if (description != null) {
                tripRequest.setDescription(description);
            }
            if (tags != null && !tags.trim().isEmpty()) {
                tripRequest.setTags(java.util.Arrays.asList(tags.split(",")));
            }
            if (latitude != null) {
                tripRequest.setLatitude(latitude);
            }
            if (longitude != null) {
                tripRequest.setLongitude(longitude);
            }
            
            log.info("Creating trip with title: {}", tripRequest.getTitle());
            
            // Validate required fields manually
            if (tripRequest.getTitle() == null || tripRequest.getTitle().trim().isEmpty()) {
                log.warn("Trip title is empty");
                return ResponseEntity.badRequest().build();
            }
            
            // Combine photos from both sources (photos and primaryImage/additionalImages)
            // Priority: primaryImage > additionalImages > photos
            MultipartFile primaryImageToUpload = null;
            List<MultipartFile> additionalImagesToUpload = new ArrayList<>();
            
            if (primaryImage != null && !primaryImage.isEmpty()) {
                primaryImageToUpload = primaryImage;
            }
            
            if (additionalImages != null) {
                additionalImages.stream()
                        .filter(file -> file != null && !file.isEmpty())
                        .forEach(additionalImagesToUpload::add);
            }
            
            if (photos != null) {
                for (MultipartFile file : photos) {
                    if (file != null && !file.isEmpty()) {
                        if (primaryImageToUpload == null) {
                            primaryImageToUpload = file;
                        } else {
                            additionalImagesToUpload.add(file);
                        }
                    }
                }
            }
            
            TripResponse response = tripService.createTripForCurrentUser(
                    tripRequest, 
                    primaryImageToUpload,
                    additionalImagesToUpload.isEmpty() ? null : additionalImagesToUpload);
            log.info("Trip created successfully with ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            log.error("Authentication error creating trip", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Error creating trip: {}", e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    @PostMapping(value = "/trips/author/{authorId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TripResponse> createTripForAuthorWithUploads(
            @PathVariable Long authorId,
            @Valid @RequestPart("trip") TripRequest request,
            @RequestPart(value = "primaryImage", required = false) MultipartFile primaryImage,
            @RequestPart(value = "additionalImages", required = false) List<MultipartFile> additionalImages) {
        TripResponse response = tripService.createTripForAuthorWithUploads(authorId, request, primaryImage, additionalImages);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/trips/my", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TripResponse> createMyTripWithImages(
            @RequestPart("trip") TripRequest request,
            @RequestPart(value = "primaryImage", required = false) MultipartFile primaryImage,
            @RequestPart(value = "additionalImages", required = false) List<MultipartFile> additionalImages) {
        // Validate required fields manually
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        TripResponse response = tripService.createTripForCurrentUser(request, primaryImage, additionalImages);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Alias endpoint for frontend compatibility (destinations = trips)
    @PostMapping(value = "/destinations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TripResponse> createDestination(
            @RequestPart(value = "trip", required = false) String tripJson,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestPart(value = "primaryImage", required = false) MultipartFile primaryImage,
            @RequestPart(value = "additionalImages", required = false) List<MultipartFile> additionalImages) {
        try {
            TripRequest tripRequest = new TripRequest();
            
            // Try to parse JSON first
            if (tripJson != null && !tripJson.trim().isEmpty()) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    tripRequest = objectMapper.readValue(tripJson, TripRequest.class);
                } catch (Exception e) {
                    log.warn("Failed to parse trip JSON, using form params instead: {}", e.getMessage());
                }
            }
            
            // Override with form params if provided (form params take precedence)
            if (title != null && !title.trim().isEmpty()) {
                tripRequest.setTitle(title);
            }
            if (description != null) {
                tripRequest.setDescription(description);
            }
            if (tags != null && !tags.trim().isEmpty()) {
                tripRequest.setTags(java.util.Arrays.asList(tags.split(",")));
            }
            if (latitude != null) {
                tripRequest.setLatitude(latitude);
            }
            if (longitude != null) {
                tripRequest.setLongitude(longitude);
            }
            
            log.info("Creating destination with title: {}", tripRequest.getTitle());
            
            // Validate required fields manually
            if (tripRequest.getTitle() == null || tripRequest.getTitle().trim().isEmpty()) {
                log.warn("Destination title is empty");
                return ResponseEntity.badRequest().build();
            }
            
            TripResponse response = tripService.createTripForCurrentUser(tripRequest, primaryImage, additionalImages);
            log.info("Destination created successfully with ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            log.error("Authentication error creating destination", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Error creating destination: {}", e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle it
        }
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
    
    @PutMapping(value = "/trips/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable Long id,
            @Valid @RequestBody TripRequest request) {
        TripResponse response = tripService.updateTrip(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/trips/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TripResponse> updateTripWithUploads(
            @PathVariable Long id,
            @RequestPart(value = "trip", required = false) String tripJson,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestPart(value = "primaryImage", required = false) MultipartFile primaryImage,
            @RequestPart(value = "additionalImages", required = false) List<MultipartFile> additionalImages) {
        try {
            TripRequest tripRequest = new TripRequest();
            
            // Try to parse JSON first
            if (tripJson != null && !tripJson.trim().isEmpty()) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    tripRequest = objectMapper.readValue(tripJson, TripRequest.class);
                } catch (Exception e) {
                    log.warn("Failed to parse trip JSON, using form params instead: {}", e.getMessage());
                }
            }
            
            // Override with form params if provided (form params take precedence)
            if (title != null && !title.trim().isEmpty()) {
                tripRequest.setTitle(title);
            }
            if (description != null) {
                tripRequest.setDescription(description);
            }
            if (tags != null && !tags.trim().isEmpty()) {
                tripRequest.setTags(java.util.Arrays.asList(tags.split(",")));
            }
            if (latitude != null) {
                tripRequest.setLatitude(latitude);
            }
            if (longitude != null) {
                tripRequest.setLongitude(longitude);
            }
            
            log.info("Updating trip with ID: {}", id);
            
            TripResponse response = tripService.updateTripWithUploads(
                    id,
                    tripRequest,
                    primaryImage,
                    additionalImages);
            log.info("Trip ID {} updated successfully", id);
            return ResponseEntity.ok(response);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            log.error("Authentication error updating trip", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Error updating trip: {}", e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    // Alias endpoint for frontend compatibility (destinations = trips)
    @PutMapping(value = "/destinations/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TripResponse> updateDestination(
            @PathVariable Long id,
            @RequestPart(value = "trip", required = false) String tripJson,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestPart(value = "primaryImage", required = false) MultipartFile primaryImage,
            @RequestPart(value = "additionalImages", required = false) List<MultipartFile> additionalImages) {
        try {
            TripRequest tripRequest = new TripRequest();
            
            // Try to parse JSON first
            if (tripJson != null && !tripJson.trim().isEmpty()) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    tripRequest = objectMapper.readValue(tripJson, TripRequest.class);
                } catch (Exception e) {
                    log.warn("Failed to parse trip JSON, using form params instead: {}", e.getMessage());
                }
            }
            
            // Override with form params if provided (form params take precedence)
            if (title != null && !title.trim().isEmpty()) {
                tripRequest.setTitle(title);
            }
            if (description != null) {
                tripRequest.setDescription(description);
            }
            if (tags != null && !tags.trim().isEmpty()) {
                tripRequest.setTags(java.util.Arrays.asList(tags.split(",")));
            }
            if (latitude != null) {
                tripRequest.setLatitude(latitude);
            }
            if (longitude != null) {
                tripRequest.setLongitude(longitude);
            }
            
            log.info("Updating destination with ID: {}", id);
            
            TripResponse response = tripService.updateTripWithUploads(
                    id,
                    tripRequest,
                    primaryImage,
                    additionalImages);
            log.info("Destination ID {} updated successfully", id);
            return ResponseEntity.ok(response);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            log.error("Authentication error updating destination", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Error updating destination: {}", e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    @PutMapping(value = "/destinations/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TripResponse> updateDestinationJson(
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

