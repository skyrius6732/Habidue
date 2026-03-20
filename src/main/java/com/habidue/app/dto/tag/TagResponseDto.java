package com.habidue.app.dto.tag;

import com.habidue.app.domain.tag.Tag;
import com.habidue.app.domain.tag.TagType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TagResponseDto {
    private Long id;
    private String name;
    private TagType type;

    public TagResponseDto(Tag tag) {
        this.id = tag.getId();
        this.name = tag.getName();
        this.type = tag.getType();
    }
}
