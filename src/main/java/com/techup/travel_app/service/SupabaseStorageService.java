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
        if (photos == null) {
            return uploaded;
        }

        for (MultipartFile photo : photos) {
            if (photo == null || photo.isEmpty()) {
                continue;
            }
            uploaded.add(uploadPhoto(photo));
        }

        return uploaded;
    }

    private String uploadPhoto(MultipartFile file) {
        validateConfiguration();

        try {
            String objectName = buildObjectName(file.getOriginalFilename());
            String requestUrl = normalizeBaseUrl() + "/storage/v1/object/" + bucketName + "/" + objectName;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.set("apikey", apiKey);
            headers.add("x-upsert", "true");
            headers.setContentType(resolveMediaType(file.getContentType()));

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to upload photo to Supabase. Status: {}, body: {}", response.getStatusCode(), response.getBody());
                throw new IllegalStateException("Unable to upload photo to Supabase storage");
            }

            return normalizeBaseUrl() + "/storage/v1/object/public/" + bucketName + "/" + objectName;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read photo bytes", ex);
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

