package com.habidue.app.wedding.dto;

import com.habidue.app.wedding.domain.RsvpSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WeddingRsvpRequestDto {

    @NotBlank
    private String name;

    private String phone;

    @NotNull
    private Boolean attendance;

    private int guestCount = 1;

    private Boolean mealOption;

    private RsvpSide side = RsvpSide.UNKNOWN;

    private String message;
}
