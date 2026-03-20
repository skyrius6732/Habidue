package com.habidue.app.dto.tag;

import com.habidue.app.domain.tag.Tag;
import com.habidue.app.domain.tag.TagType;
import com.habidue.app.domain.tag.UserTag;
import lombok.Getter;

@Getter
public class UserTagResponseDto {
    private Long id;      // user_tag 테이블의 PK
    private Long tagId;   // tag 테이블의 ID
    private String name;  // 태그명 (서울, 청년 등)
    private TagType type; // 태그 타입 (REGION, TYPE 등)

    public UserTagResponseDto(UserTag userTag) {
        this.id = userTag.getId();
        this.tagId = userTag.getTag().getId();
        this.name = userTag.getTag().getName();
        this.type = userTag.getTag().getType();
    }

    // Tag 엔티티에서 바로 변환할 때 사용 (검색 결과용)
    public UserTagResponseDto(Tag tag) {
        this.tagId = tag.getId();
        this.name = tag.getName();
        this.type = tag.getType();
    }
}
