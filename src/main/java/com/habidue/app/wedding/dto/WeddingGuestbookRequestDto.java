package com.habidue.app.wedding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WeddingGuestbookRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    private String password;

    @NotBlank
    private String message;
}
