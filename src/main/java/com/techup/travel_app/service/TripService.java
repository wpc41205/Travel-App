package com.techup.travel_app.service;

import com.techup.travel_app.dto.TripRequest;
import com.techup.travel_app.dto.TripResponse;
import com.techup.travel_app.entity.Trip;
import com.techup.travel_app.entity.User;
import com.techup.travel_app.repository.TripRepository;
import com.techup.travel_app.repository.UserRepository;
import com.techup.travel_app.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {
    
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final SupabaseStorageService storageService;
    
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

    @Transactional
    public TripResponse createTripWithUploads(TripRequest request, List<MultipartFile> photos) {
        List<String> uploadedPhotos = storageService.uploadTripPhotos(photos);
        request.setPhotos(uploadedPhotos);
        return createTrip(request);
    }

    @Transactional
    public TripResponse createTripForAuthorWithUploads(
            Long authorId,
            TripRequest request,
            MultipartFile primaryImage,
            List<MultipartFile> additionalImages) {
        request.setAuthorId(authorId);

        List<MultipartFile> filesToUpload = new ArrayList<>();
        if (primaryImage != null && !primaryImage.isEmpty()) {
            filesToUpload.add(primaryImage);
        }
        if (additionalImages != null) {
            additionalImages.stream()
                    .filter(file -> file != null && !file.isEmpty())
                    .forEach(filesToUpload::add);
        }

        List<String> uploadedPhotos = storageService.uploadTripPhotos(filesToUpload);
        request.setPhotos(uploadedPhotos);
        return createTrip(request);
    }

    @Transactional
    public TripResponse createTripForCurrentUser(
            TripRequest request,
            MultipartFile primaryImage,
            List<MultipartFile> additionalImages) {
        // Get current authenticated user
        Long currentUserId = getCurrentUserId();
        
        // Verify user exists
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + currentUserId));
        
        // Combine primary and additional images for upload
        List<MultipartFile> filesToUpload = new ArrayList<>();
        if (primaryImage != null && !primaryImage.isEmpty()) {
            filesToUpload.add(primaryImage);
        }
        if (additionalImages != null) {
            additionalImages.stream()
                    .filter(file -> file != null && !file.isEmpty())
                    .forEach(filesToUpload::add);
        }
        
        // Upload images to Supabase storage
        List<String> uploadedPhotos = new ArrayList<>();
        if (!filesToUpload.isEmpty()) {
            log.info("Uploading {} image(s) to Supabase Storage for trip: {}", filesToUpload.size(), request.getTitle());
            uploadedPhotos = storageService.uploadTripPhotos(filesToUpload);
            log.info("Successfully uploaded {} image(s) to Supabase Storage. URLs: {}", uploadedPhotos.size(), uploadedPhotos);
        } else {
            log.info("No images to upload for trip: {}", request.getTitle());
        }
        
        // Create trip with current user as author
        Trip trip = new Trip();
        trip.setTitle(request.getTitle());
        trip.setDescription(request.getDescription());
        trip.setPhotos(uploadedPhotos);
        trip.setTags(request.getTags() != null ? request.getTags() : new ArrayList<>());
        trip.setLatitude(request.getLatitude());
        trip.setLongitude(request.getLongitude());
        trip.setAuthorId(currentUserId);
        
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
        
        Long currentUserId = getCurrentUserId();
        if (trip.getAuthorId() == null || !trip.getAuthorId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only edit your own trips.");
        }
        
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
    public TripResponse updateTripWithUploads(
            Long id,
            TripRequest request,
            MultipartFile primaryImage,
            List<MultipartFile> additionalImages) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));
        
        Long currentUserId = getCurrentUserId();
        if (trip.getAuthorId() == null || !trip.getAuthorId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only edit your own trips.");
        }
        
        // Handle image uploads - separate primary and additional images
        String uploadedPrimaryImageUrl = null;
        List<String> uploadedAdditionalImageUrls = new ArrayList<>();
        
        if (primaryImage != null && !primaryImage.isEmpty()) {
            List<MultipartFile> primaryFileList = new ArrayList<>();
            primaryFileList.add(primaryImage);
            List<String> uploaded = storageService.uploadTripPhotos(primaryFileList);
            if (!uploaded.isEmpty()) {
                uploadedPrimaryImageUrl = uploaded.get(0);
                log.info("Uploaded primary image to Supabase Storage for trip ID: {}", id);
            }
        }
        
        if (additionalImages != null && !additionalImages.isEmpty()) {
            List<MultipartFile> additionalFiles = additionalImages.stream()
                    .filter(file -> file != null && !file.isEmpty())
                    .collect(Collectors.toList());
            if (!additionalFiles.isEmpty()) {
                uploadedAdditionalImageUrls = storageService.uploadTripPhotos(additionalFiles);
                log.info("Uploaded {} additional image(s) to Supabase Storage for trip ID: {}", 
                        uploadedAdditionalImageUrls.size(), id);
            }
        }
        
        // Update trip fields
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            trip.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            trip.setDescription(request.getDescription());
        }
        if (request.getTags() != null) {
            trip.setTags(request.getTags());
        }
        if (request.getLatitude() != null) {
            trip.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            trip.setLongitude(request.getLongitude());
        }
        
        // Update photos: merge new uploads with existing photos
        // Strategy: 
        // 1. Start with existing photos from request (if provided) or current trip photos
        // 2. If primary image is uploaded, replace the first photo (or add as first if no photos exist)
        // 3. Add additional uploaded images to the end
        
        List<String> finalPhotos = new ArrayList<>();
        
        // Start with existing photos: use request.getPhotos() if provided (frontend sends current photos to keep)
        // Otherwise, use current trip photos (preserve all existing photos)
        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            // Frontend sent the list of photos to keep (may have removed some via delete button)
            finalPhotos.addAll(request.getPhotos());
            log.info("Using {} existing photo(s) from request", request.getPhotos().size());
        } else if (trip.getPhotos() != null && !trip.getPhotos().isEmpty()) {
            // No photos in request, keep all existing photos
            finalPhotos.addAll(trip.getPhotos());
            log.info("Keeping {} existing photo(s) from trip", trip.getPhotos().size());
        }
        
        // Handle primary image upload: replace first photo or add as first
        if (uploadedPrimaryImageUrl != null) {
            if (!finalPhotos.isEmpty()) {
                // Replace first photo with new primary image
                finalPhotos.set(0, uploadedPrimaryImageUrl);
                log.info("Replaced primary image (first photo)");
            } else {
                // No existing photos, add primary as first
                finalPhotos.add(uploadedPrimaryImageUrl);
                log.info("Added primary image as first photo");
            }
        }
        
        // Handle additional images upload: append to the end
        if (!uploadedAdditionalImageUrls.isEmpty()) {
            for (String additionalUrl : uploadedAdditionalImageUrls) {
                if (!finalPhotos.contains(additionalUrl)) {
                    finalPhotos.add(additionalUrl);
                }
            }
            log.info("Added {} additional image(s), total photos: {}", 
                    uploadedAdditionalImageUrls.size(), finalPhotos.size());
        }
        
        trip.setPhotos(finalPhotos);
        
        Trip updatedTrip = tripRepository.save(trip);
        log.info("Trip ID {} updated successfully", id);
        return mapToResponse(updatedTrip);
    }
    
    @Transactional
    public void deleteTrip(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

        Long currentUserId = getCurrentUserId();
        if (trip.getAuthorId() == null || !trip.getAuthorId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only delete your own trips.");
        }

        tripRepository.delete(trip);
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

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        throw new AccessDeniedException("Unable to determine current user.");
    }
}

