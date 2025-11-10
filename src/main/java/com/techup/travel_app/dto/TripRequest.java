package com.techup.travel_app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TripRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    private List<String> photos = new ArrayList<>();
    
    private List<String> tags = new ArrayList<>();
    
    private Double latitude;
    
    private Double longitude;
    
    @NotNull(message = "Author ID is required")
    private Long authorId;
}

