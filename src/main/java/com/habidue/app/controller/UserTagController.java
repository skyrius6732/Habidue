package com.habidue.app.controller;

import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.tag.UserTagRequestDto;
import com.habidue.app.dto.tag.UserTagResponseDto;
import com.habidue.app.service.tag.UserTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-tags")
@RequiredArgsConstructor
public class UserTagController {

    private final UserTagService userTagService;

    // 1. 태그 검색 (자동완성)
    @GetMapping("/search")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<List<UserTagResponseDto>>> searchTags(@RequestParam String name) {
        List<UserTagResponseDto> results = userTagService.searchTags(name).stream()
                .map(UserTagResponseDto::new)
                .collect(Collectors.toList());
        return ApiResponse.success(results);
    }

    // 2. 내 관심 태그 목록 조회
    @GetMapping
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<List<UserTagResponseDto>>> getMyTags() {
        List<UserTagResponseDto> results = userTagService.getMyTags().stream()
                .map(UserTagResponseDto::new)
                .collect(Collectors.toList());
        return ApiResponse.success(results);
    }

    // 3. 관심 태그 추가 (ID 기준)
    @PostMapping("/{tagId}")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<UserTagResponseDto>> addUserTag(@PathVariable Long tagId) {
        return ApiResponse.success(HttpStatus.CREATED, new UserTagResponseDto(userTagService.addUserTag(tagId)));
    }

    // 3-1. 관심 태그 추가 (이름 기반)
    @PostMapping
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<UserTagResponseDto>> addUserTagByName(@RequestBody UserTagRequestDto requestDto) {
        return ApiResponse.success(HttpStatus.CREATED, new UserTagResponseDto(userTagService.addUserTagByName(requestDto)));
    }

    // 4. 관심 태그 삭제 (ID 기준)
    @DeleteMapping("/{id}")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> deleteUserTag(@PathVariable Long id) {
        userTagService.deleteUserTag(id);
        return ApiResponse.success(HttpStatus.NO_CONTENT, null);
    }

    // 4-1. 관심 태그 삭제 (이름 기준)
    @DeleteMapping
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> deleteUserTagByName(
            @RequestParam String name, 
            @RequestParam com.habidue.app.domain.tag.TagType type) {
        userTagService.deleteUserTagByName(name, type);
        return ApiResponse.success(HttpStatus.NO_CONTENT, null);
    }
}
