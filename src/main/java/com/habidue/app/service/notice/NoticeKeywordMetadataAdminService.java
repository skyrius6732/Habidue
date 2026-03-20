package com.habidue.app.service.notice;

import com.habidue.app.domain.notice.NoticeKeywordMetadata;
import com.habidue.app.dto.notice.NoticeKeywordMetadataRequestDto;
import com.habidue.app.dto.notice.NoticeKeywordMetadataResponseDto;
import com.habidue.app.repository.notice.NoticeKeywordMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeKeywordMetadataAdminService {

    private final NoticeKeywordMetadataRepository metadataRepository;

    public List<NoticeKeywordMetadataResponseDto> getAllMetadata() {
        return metadataRepository.findAll().stream()
                .map(NoticeKeywordMetadataResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public NoticeKeywordMetadataResponseDto createMetadata(NoticeKeywordMetadataRequestDto dto) {
        NoticeKeywordMetadata metadata = NoticeKeywordMetadata.builder()
                .keyword(dto.getKeyword())
                .tagType(dto.getTagType())
                .targetStatus(dto.getTargetStatus())
                .representativeName(dto.getRepresentativeName())
                .build();
        return new NoticeKeywordMetadataResponseDto(metadataRepository.save(metadata));
    }

    @Transactional
    public NoticeKeywordMetadataResponseDto updateMetadata(Long id, NoticeKeywordMetadataRequestDto dto) {
        NoticeKeywordMetadata metadata = metadataRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Metadata not found for ID: " + id));

        metadata.setKeyword(dto.getKeyword());
        metadata.setTagType(dto.getTagType());
        metadata.setTargetStatus(dto.getTargetStatus());
        metadata.setRepresentativeName(dto.getRepresentativeName());

        return new NoticeKeywordMetadataResponseDto(metadataRepository.save(metadata));
    }

    @Transactional
    public void deleteMetadata(Long id) {
        metadataRepository.deleteById(id);
    }
}
