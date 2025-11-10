package com.techup.travel_app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {
    
    private Long id;
    private String title;
    private String description;
    private List<String> photos = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private Double latitude;
    private Double longitude;
    private Long authorId;
    private String authorEmail;
    private String authorDisplayName;
    private Instant createdAt;
    private Instant updatedAt;
}

