package com.habidue.app.wedding.dto;

import com.habidue.app.wedding.domain.WeddingGuestbook;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class WeddingGuestbookResponseDto {

    private final Long id;
    private final String name;
    private final String message;
    private final LocalDateTime createdAt;

    public WeddingGuestbookResponseDto(WeddingGuestbook guestbook) {
        this.id = guestbook.getId();
        this.name = guestbook.getName();
        this.message = guestbook.getMessage();
        this.createdAt = guestbook.getCreatedAt();
    }
}
