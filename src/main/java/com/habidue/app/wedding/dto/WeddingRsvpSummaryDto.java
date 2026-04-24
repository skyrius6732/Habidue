package com.habidue.app.wedding.dto;

import com.habidue.app.wedding.domain.WeddingRsvp;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class WeddingRsvpSummaryDto {

    private final long totalResponses;
    private final long attendingCount;
    private final long notAttendingCount;
    private final long totalGuests;
    private final List<RsvpItemDto> items;

    public WeddingRsvpSummaryDto(List<WeddingRsvp> rsvps) {
        this.totalResponses = rsvps.size();
        this.attendingCount = rsvps.stream().filter(WeddingRsvp::isAttendance).count();
        this.notAttendingCount = rsvps.stream().filter(r -> !r.isAttendance()).count();
        this.totalGuests = rsvps.stream()
                .filter(WeddingRsvp::isAttendance)
                .mapToLong(WeddingRsvp::getGuestCount)
                .sum();
        this.items = rsvps.stream().map(RsvpItemDto::new).collect(Collectors.toList());
    }

    @Getter
    public static class RsvpItemDto {
        private final Long id;
        private final String name;
        private final String phone;
        private final boolean attendance;
        private final int guestCount;
        private final Boolean mealOption;
        private final String side;
        private final String message;
        private final LocalDateTime createdAt;

        public RsvpItemDto(WeddingRsvp rsvp) {
            this.id = rsvp.getId();
            this.name = rsvp.getName();
            this.phone = rsvp.getPhone();
            this.attendance = rsvp.isAttendance();
            this.guestCount = rsvp.getGuestCount();
            this.mealOption = rsvp.getMealOption();
            this.side = rsvp.getSide().name();
            this.message = rsvp.getMessage();
            this.createdAt = rsvp.getCreatedAt();
        }
    }
}
