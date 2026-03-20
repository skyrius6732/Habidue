package com.habidue.app.dto.notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeWakeUpStatusDto {
    private long currentCount;
    private long targetCount;
    
    @com.fasterxml.jackson.annotation.JsonProperty("isClicked")
    private boolean isClicked;
    
    @com.fasterxml.jackson.annotation.JsonProperty("isRevived")
    private boolean isRevived;
}
