package com.techup.travel_app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseStorageService {

    private final RestTemplate restTemplate;

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.bucket:}")
    private String bucketName;

    @Value("${supabase.apiKey:}")
    private String apiKey;

    public List<String> uploadTripPhotos(List<MultipartFile> photos) {
        List<String> uploaded = new ArrayList<>();
        if (photos == null || photos.isEmpty()) {
            log.info("No photos to upload");
            return uploaded;
        }

        log.info("Starting upload of {} photo(s) to Supabase Storage", photos.size());
        
        for (MultipartFile photo : photos) {
            if (photo == null || photo.isEmpty()) {
                log.warn("Skipping empty photo");
                continue;
            }
            
            try {
                String url = uploadPhoto(photo);
                uploaded.add(url);
                log.info("Successfully uploaded photo: {} -> {}", photo.getOriginalFilename(), url);
            } catch (Exception e) {
                log.error("Failed to upload photo: {}", photo.getOriginalFilename(), e);
                throw new IllegalStateException("Failed to upload photo: " + photo.getOriginalFilename() + " - " + e.getMessage(), e);
            }
        }

        log.info("Successfully uploaded {} photo(s) to Supabase Storage", uploaded.size());
        return uploaded;
    }

    private String uploadPhoto(MultipartFile file) {
        validateConfiguration();

        try {
            String objectName = buildObjectName(file.getOriginalFilename());
            String requestUrl = normalizeBaseUrl() + "/storage/v1/object/" + bucketName + "/" + objectName;
            
            log.info("Uploading file to Supabase: {} -> {}", file.getOriginalFilename(), objectName);
            log.info("File size: {} bytes, Content type: {}", file.getSize(), file.getContentType());
            log.info("Supabase URL: {}, Bucket: {}", supabaseUrl, bucketName);

            HttpHeaders headers = new HttpHeaders();
            // Use Bearer token for authentication (works with both anon and service role keys)
            headers.setBearerAuth(apiKey);
            headers.set("apikey", apiKey);
            headers.add("x-upsert", "true");
            headers.setContentType(resolveMediaType(file.getContentType()));
            // Add cache control
            headers.add("Cache-Control", "max-age=3600");

            byte[] fileBytes = file.getBytes();
            HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);

            log.info("Sending POST request to: {}", requestUrl);
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.POST, entity, String.class);

            log.info("Supabase response status: {}, body: {}", response.getStatusCode(), response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorBody = response.getBody() != null ? response.getBody() : "No error body";
                log.error("Failed to upload photo to Supabase. Status: {}, body: {}", response.getStatusCode(), errorBody);
                
                // Provide helpful error message
                if (response.getStatusCode().value() == 401 || response.getStatusCode().value() == 403) {
                    throw new IllegalStateException(
                        "Authentication failed. Please check your Supabase API key. " +
                        "For file uploads, you need to use a Service Role Key (not anon key). " +
                        "Status: " + response.getStatusCode() + ", Body: " + errorBody
                    );
                }
                
                throw new IllegalStateException(
                    "Unable to upload photo to Supabase storage. Status: " + response.getStatusCode() + 
                    ", Body: " + errorBody
                );
            }

            String publicUrl = normalizeBaseUrl() + "/storage/v1/object/public/" + bucketName + "/" + objectName;
            log.info("Upload successful! Public URL: {}", publicUrl);
            return publicUrl;
        } catch (org.springframework.web.client.RestClientException ex) {
            log.error("RestClient error uploading photo to Supabase: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Failed to connect to Supabase storage: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            log.error("IO error reading photo bytes: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Failed to read photo bytes: " + ex.getMessage(), ex);
        }
    }

    private MediaType resolveMediaType(String contentType) {
        if (StringUtils.hasText(contentType)) {
            return MediaType.parseMediaType(contentType);
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String buildObjectName(String originalFilename) {
        String sanitizedName = "photo";
        if (StringUtils.hasText(originalFilename)) {
            sanitizedName = originalFilename.trim();
        }
        sanitizedName = sanitizedName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        return "trips/" + UUID.randomUUID() + "-" + sanitizedName;
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(supabaseUrl) || !StringUtils.hasText(bucketName) || !StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Supabase configuration is missing. Please set SUPABASE_URL, SUPABASE_BUCKET and SUPABASE_API_KEY.");
        }
    }

    private String normalizeBaseUrl() {
        String base = supabaseUrl.trim();
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }
}

