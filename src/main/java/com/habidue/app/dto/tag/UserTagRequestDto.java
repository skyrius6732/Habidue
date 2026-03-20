package com.habidue.app.dto.tag;

import com.habidue.app.domain.tag.TagType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserTagRequestDto {
    private String name;
    private TagType type;
}
