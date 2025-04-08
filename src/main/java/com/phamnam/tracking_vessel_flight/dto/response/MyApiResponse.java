package com.phamnam.tracking_vessel_flight.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> MyApiResponse<T> success(T data) {
        return MyApiResponse.<T>builder()
                .success(true)
                .message("Operation successful")
                .data(data)
                .build();
    }

    public static <T> MyApiResponse<T> success(T data, String message) {
        return MyApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> MyApiResponse<T> error(String message) {
        return MyApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
