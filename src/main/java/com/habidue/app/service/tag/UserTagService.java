package com.habidue.app.service.tag;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.tag.Tag;
import com.habidue.app.domain.tag.UserTag;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.tag.UserTagRequestDto;
import com.habidue.app.repository.tag.TagRepository;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.repository.tag.UserTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTagService {

    private final UserTagRepository userTagRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("인증된 사용자를 찾을 수 없습니다.");
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new NoSuchElementException("인증된 사용자 정보를 DB에서 찾을 수 없습니다."));
    }

    // 1. 전체 태그 검색 (자동완성용)
    public List<Tag> searchTags(String name) {
        return tagRepository.findByNameContaining(name);
    }

    // 2. 내 관심 태그 목록 조회
    public List<UserTag> getMyTags() {
        User currentUser = getCurrentUser();
        return userTagRepository.findByUserId(currentUser.getId());
    }

    // 3. 관심 태그 추가 (ID 기준)
    @Transactional
    public UserTag addUserTag(Long tagId) {
        User currentUser = getCurrentUser();
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 태그입니다. ID: " + tagId));

        return saveUserTag(currentUser, tag);
    }

    // 3-1. 관심 태그 추가 (이름/타입 기준)
    @Transactional
    public UserTag addUserTagByName(UserTagRequestDto requestDto) {
        User currentUser = getCurrentUser();
        Tag tag = tagRepository.findByNameAndType(requestDto.getName(), requestDto.getType())
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 태그입니다: " + requestDto.getName()));

        return saveUserTag(currentUser, tag);
    }

    private UserTag saveUserTag(User currentUser, Tag tag) {
        if (userTagRepository.existsByUserIdAndTagId(currentUser.getId(), tag.getId())) {
            throw new IllegalArgumentException("이미 등록된 관심 태그입니다.");
        }

        UserTag userTag = UserTag.builder()
                .user(currentUser)
                .tag(tag)
                .build();

        return userTagRepository.save(userTag);
    }

    // 4. 관심 태그 삭제 (ID 기준)
    @Transactional
    public void deleteUserTag(Long userTagId) {
        User currentUser = getCurrentUser();
        UserTag userTag = userTagRepository.findById(userTagId)
                .orElseThrow(() -> new NoSuchElementException("관심 태그 정보를 찾을 수 없습니다."));

        if (!userTag.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("해당 정보를 삭제할 권한이 없습니다.");
        }

        userTagRepository.delete(userTag);
    }

    // 4-1. 관심 태그 삭제 (이름/타입 기준)
    @Transactional
    public void deleteUserTagByName(String name, com.habidue.app.domain.tag.TagType type) {
        User currentUser = getCurrentUser();
        Tag tag = tagRepository.findByNameAndType(name, type)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 태그입니다: " + name));

        UserTag userTag = userTagRepository.findByUserIdAndTagId(currentUser.getId(), tag.getId())
                .orElseThrow(() -> new NoSuchElementException("등록되지 않은 관심 태그입니다."));

        userTagRepository.delete(userTag);
    }
}
