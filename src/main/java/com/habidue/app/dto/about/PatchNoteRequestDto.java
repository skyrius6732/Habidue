package com.habidue.app.dto.about;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatchNoteRequestDto {
    private String title;   // 제목 추가
    private String version;
    private String date;
    private List<String> details;
}
