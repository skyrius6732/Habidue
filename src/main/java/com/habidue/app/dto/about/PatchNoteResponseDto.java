package com.habidue.app.dto.about;

import com.habidue.app.domain.about.PatchNote;
import com.habidue.app.domain.about.PatchNoteDetail;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatchNoteResponseDto {
    private Long id;
    private String title;
    private String version;
    private String date;
    private List<String> details;

    public PatchNoteResponseDto(PatchNote entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.version = entity.getVersion();
        this.date = entity.getDate();
        this.details = entity.getDetails().stream()
                .map(PatchNoteDetail::getContent)
                .collect(Collectors.toList());
    }
}
