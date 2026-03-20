package com.habidue.app.dto.notice;

import com.habidue.app.domain.notice.NoticeKeywordMetadata;
import com.habidue.app.domain.notice.NoticeStatus;
import com.habidue.app.domain.tag.TagType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NoticeKeywordMetadataRequestDto {
    private String keyword;
    private TagType tagType;
    private NoticeStatus targetStatus;
    private String representativeName;

    public NoticeKeywordMetadata toEntity() {
        return NoticeKeywordMetadata.builder()
                .keyword(keyword)
                .tagType(tagType)
                .targetStatus(targetStatus)
                .representativeName(representativeName)
                .build();
    }
}
