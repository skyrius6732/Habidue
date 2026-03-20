package com.habidue.app.controller.admin;

import com.habidue.app.domain.tag.Tag;
import com.habidue.app.domain.tag.TagType;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.tag.TagResponseDto;
import com.habidue.app.service.tag.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
@Secured("ROLE_ADMIN")
public class TagAdminController {

    private final TagService tagService;

    /**
     * 전체 태그 목록 조회 (관리자 전용)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TagResponseDto>>> getAllTags() {
        return ApiResponse.success(tagService.getAllTags());
    }

    @GetMapping("/{tagId}/usage")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> getTagUsage(@PathVariable Long tagId) {
        return ApiResponse.success(tagService.getTagUsage(tagId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TagResponseDto>> createTag(
            @RequestParam String name,
            @RequestParam TagType type) {
        Tag newTag = tagService.createTag(name, type);
        return ApiResponse.success(new TagResponseDto(newTag));
    }

    /**
     * 태그 정보 수정 (이름, 타입)
     */
    @PatchMapping("/{tagId}")
    public ResponseEntity<ApiResponse<TagResponseDto>> updateTag(
            @PathVariable Long tagId,
            @RequestParam String name,
            @RequestParam TagType type) {
        Tag updatedTag = tagService.updateTag(tagId, name, type);
        return ApiResponse.success(new TagResponseDto(updatedTag));
    }

    /**
     * 태그 삭제
     */
    @DeleteMapping("/{tagId}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
        return ApiResponse.success(null);
    }
}
