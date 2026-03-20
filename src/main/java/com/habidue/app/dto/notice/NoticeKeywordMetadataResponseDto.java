package com.habidue.app.dto.notice;

import com.habidue.app.domain.notice.NoticeKeywordMetadata;
import com.habidue.app.domain.notice.NoticeStatus;
import com.habidue.app.domain.tag.TagType;
import lombok.Getter;

@Getter
public class NoticeKeywordMetadataResponseDto {
    private Long id;
    private String keyword;
    private TagType tagType;
    private NoticeStatus targetStatus;
    private String representativeName;

    public NoticeKeywordMetadataResponseDto(NoticeKeywordMetadata metadata) {
        this.id = metadata.getId();
        this.keyword = metadata.getKeyword();
        this.tagType = metadata.getTagType();
        this.targetStatus = metadata.getTargetStatus();
        this.representativeName = metadata.getRepresentativeName();
    }
}
