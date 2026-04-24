package com.habidue.app.wedding.dto;

import com.habidue.app.wedding.domain.AccountSide;
import com.habidue.app.wedding.domain.InvitationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class WeddingInvitationRequestDto {

    @NotBlank
    private String slug;

    @NotBlank
    private String groomName;

    @NotBlank
    private String brideName;

    private String groomFatherName;
    private String groomMotherName;
    private String brideFatherName;
    private String brideMotherName;

    @NotNull
    private LocalDateTime weddingDate;

    @NotBlank
    private String venueName;

    @NotBlank
    private String venueAddress;

    private String venueDetailAddress;
    private Double venueLat;
    private Double venueLng;

    private String greetingMessage;
    private String transportInfo;
    private String parkingInfo;
    private String musicUrl;
    private boolean musicAutoPlay;
    private String template;

    private InvitationStatus status;
    private LocalDateTime expiresAt;

    private List<Long> deletedPhotoIds;
    private List<AccountDto> accounts;

    @Getter
    @NoArgsConstructor
    public static class AccountDto {
        private AccountSide side;
        private String bankName;
        private String accountNumber;
        private String accountHolder;
        private String kakaoPayLink;
    }
}
