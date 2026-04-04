package com.habidue.app.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiResponse<T> {
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status;
    private final String error;
    private final String message;
    private final T data;

    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .message("Success")
                .data(data)
                .build());
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(HttpStatus status, T data) {
        return ResponseEntity.status(status).body(ApiResponse.<T>builder()
                .status(status.value())
                .message("Success")
                .data(data)
                .build());
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message, String error) {
        return ResponseEntity.status(status).body(ApiResponse.<T>builder()
                .status(status.value())
                .error(error)
                .message(message)
                .data(null)
                .build());
    }
}
