package com.habidue.app.wedding.dto;

import com.habidue.app.wedding.domain.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class WeddingInvitationResponseDto {

    private final Long id;
    private final String slug;
    private final InvitationStatus status;
    private final String groomName;
    private final String brideName;
    private final String groomFatherName;
    private final String groomMotherName;
    private final String brideFatherName;
    private final String brideMotherName;
    private final LocalDateTime weddingDate;
    private final String venueName;
    private final String venueAddress;
    private final String venueDetailAddress;
    private final Double venueLat;
    private final Double venueLng;
    private final String greetingMessage;
    private final String transportInfo;
    private final String parkingInfo;
    private final String musicUrl;
    private final boolean musicAutoPlay;
    private final String template;
    private final int viewCount;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private final List<PhotoDto> photos;
    private final List<AccountDto> accounts;

    public WeddingInvitationResponseDto(WeddingInvitation invitation) {
        this.id = invitation.getId();
        this.slug = invitation.getSlug();
        this.status = invitation.getStatus();
        this.groomName = invitation.getGroomName();
        this.brideName = invitation.getBrideName();
        this.groomFatherName = invitation.getGroomFatherName();
        this.groomMotherName = invitation.getGroomMotherName();
        this.brideFatherName = invitation.getBrideFatherName();
        this.brideMotherName = invitation.getBrideMotherName();
        this.weddingDate = invitation.getWeddingDate();
        this.venueName = invitation.getVenueName();
        this.venueAddress = invitation.getVenueAddress();
        this.venueDetailAddress = invitation.getVenueDetailAddress();
        this.venueLat = invitation.getVenueLat();
        this.venueLng = invitation.getVenueLng();
        this.greetingMessage = invitation.getGreetingMessage();
        this.transportInfo = invitation.getTransportInfo();
        this.parkingInfo = invitation.getParkingInfo();
        this.musicUrl = invitation.getMusicUrl();
        this.musicAutoPlay = invitation.isMusicAutoPlay();
        this.template = invitation.getTemplate();
        this.viewCount = invitation.getViewCount();
        this.expiresAt = invitation.getExpiresAt();
        this.createdAt = invitation.getCreatedAt();
        this.photos = invitation.getPhotos().stream()
                .map(PhotoDto::new)
                .collect(Collectors.toList());
        this.accounts = invitation.getAccounts().stream()
                .map(AccountDto::new)
                .collect(Collectors.toList());
    }

    @Getter
    public static class PhotoDto {
        private final Long id;
        private final String imageUrl;
        private final int orderNum;

        public PhotoDto(WeddingPhoto photo) {
            this.id = photo.getId();
            this.imageUrl = photo.getImageUrl();
            this.orderNum = photo.getOrderNum();
        }
    }

    @Getter
    public static class AccountDto {
        private final Long id;
        private final AccountSide side;
        private final String bankName;
        private final String accountNumber;
        private final String accountHolder;
        private final String kakaoPayLink;

        public AccountDto(WeddingAccount account) {
            this.id = account.getId();
            this.side = account.getSide();
            this.bankName = account.getBankName();
            this.accountNumber = account.getAccountNumber();
            this.accountHolder = account.getAccountHolder();
            this.kakaoPayLink = account.getKakaoPayLink();
        }
    }
}
