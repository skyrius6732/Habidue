package com.habidue.app.controller.api;

import com.habidue.app.domain.message.Message;
import com.habidue.app.domain.user.User;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.message.MessageRequestDto;
import com.habidue.app.service.message.MessageService;
import com.habidue.app.service.user.UserService;
import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.repository.message.UserBlockRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;
    private final UserRepository userRepository; // [시니어 조치] 공개 ID 조회를 위해 추가
    private final com.habidue.app.service.board.BoardReportService boardReportService;
    private final UserBlockRepository userBlockRepository;

    private User getAuthenticatedUser(UserPrincipal principal) {
        if (principal.getUser() != null) return principal.getUser();
        return userService.getUserById(principal.getId());
    }

    @PostMapping({"", "/{receiverPublicId}"})
    public ResponseEntity<ApiResponse<Void>> sendMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable(value = "receiverPublicId", required = false) String receiverPublicId,
            @ModelAttribute MessageRequestDto requestDto,
            HttpServletRequest request) {
        
        Long receiverId = null;
        if (receiverPublicId != null && !receiverPublicId.isEmpty()) {
            receiverId = userRepository.findByPublicId(receiverPublicId)
                    .map(User::getId)
                    .orElseThrow(() -> new IllegalArgumentException("수신자 정보를 찾을 수 없습니다."));
        } else {
            String paramId = request.getParameter("receiverId");
            if (paramId != null && !paramId.isEmpty()) {
                // [시니어 조치] 파라미터로 넘어온 경우도 publicId로 취급하여 조회
                receiverId = userRepository.findByPublicId(paramId)
                        .map(User::getId)
                        .orElseThrow(() -> new IllegalArgumentException("수신자 정보를 찾을 수 없습니다."));
            }
        }

        if (receiverId == null) throw new IllegalArgumentException("수신자 정보를 확인할 수 없습니다.");
        
        String content = requestDto.getContent();
        List<MultipartFile> files = requestDto.getFiles();

        if (content == null || content.trim().isEmpty()) content = request.getParameter("content");
        if (files == null || files.isEmpty()) {
            if (request instanceof MultipartHttpServletRequest) files = ((MultipartHttpServletRequest) request).getFiles("files");
        }

        if ((content == null || content.trim().isEmpty()) && (files == null || files.isEmpty())) {
            throw new IllegalArgumentException("메시지 내용 또는 파일을 입력해주세요.");
        }
        
        messageService.sendMessage(userPrincipal.getId(), receiverId, content, files);
        return ApiResponse.success(null);
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDailyStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(messageService.getDailyMessageStatus(userPrincipal.getId()));
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<Page<com.habidue.app.dto.message.MessageResponseDto>>> getMessageRooms(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Pageable pageable) {
        User user = getAuthenticatedUser(userPrincipal);
        userService.updateUserOnlineStatus(user.getId());
        Page<Message> messages = messageService.getLatestMessagesGroupedByPartner(user, pageable);

        return ApiResponse.success(messages.map(m -> {
            User partner = (m.getSender() != null && m.getSender().getId().equals(user.getId())) ? m.getReceiver() : m.getSender();
            long unreadCount = (partner != null) ? messageService.countUnreadMessagesWithPartner(user, partner) : 0;
            boolean isOnline = (partner != null) && userService.isUserOnline(partner.getId());

            // [시니어 조치] 차단 정보 포함 (프론트엔드 리스트 필터링 및 publicId 정합성 확보용)
            boolean blockedByMe = (partner != null) && userBlockRepository.existsByBlockerAndBlocked(user, partner);
            boolean blockedByPartner = (partner != null) && userBlockRepository.existsByBlockerAndBlocked(partner, user);

            // [하이브리드] 최신 메시지에 tradeProposalId가 없으면 대화방에서 찾기
            Long roomTradeProposalId = m.getTradeProposalId();
            if (roomTradeProposalId == null && partner != null) {
                roomTradeProposalId = messageService.getConversationTradeProposalId(user, partner);
            }

            // tradeProposalId를 포함한 MessageResponseDto 생성
            var dto = com.habidue.app.dto.message.MessageResponseDto.from(m, unreadCount, isOnline, blockedByMe, blockedByPartner);
            if (roomTradeProposalId != null) {
                dto.setTradeProposalId(roomTradeProposalId);
            }
            return dto;
        }));
    }

    @GetMapping("/conversation/{partnerPublicId}")
    public ResponseEntity<ApiResponse<java.util.List<com.habidue.app.dto.message.MessageResponseDto>>> getConversation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String partnerPublicId) {
        
        User partner = userRepository.findByPublicId(partnerPublicId)
                .orElseThrow(() -> new IllegalArgumentException("대화 상대 정보를 찾을 수 없습니다."));
        Long partnerId = partner.getId();

        User user = getAuthenticatedUser(userPrincipal);
        userService.updateUserOnlineStatus(user.getId());
        
        java.util.List<Message> conversation = messageService.getConversation(user, partnerId);
        boolean isPartnerOnline = userService.isUserOnline(partnerId);
        
        // [시니어 조치] 차단 여부 조회
        boolean blockedByMe = userBlockRepository.existsByBlockerAndBlocked(user, partner);
        boolean blockedByPartner = userBlockRepository.existsByBlockerAndBlocked(partner, user);

        return ApiResponse.success(conversation.stream()
                .map(m -> com.habidue.app.dto.message.MessageResponseDto.from(m, null, isPartnerOnline, blockedByMe, blockedByPartner))
                .collect(java.util.stream.Collectors.toList()));
    }

    @PatchMapping("/conversation/{partnerPublicId}/read")
    public ResponseEntity<ApiResponse<Void>> markConversationAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String partnerPublicId) {
        User partner = userRepository.findByPublicId(partnerPublicId)
                .orElseThrow(() -> new IllegalArgumentException("대화 상대 정보를 찾을 수 없습니다."));
        messageService.markConversationAsRead(getAuthenticatedUser(userPrincipal), partner.getId());
        return ApiResponse.success(null);
    }

    @PostMapping("/active")
    public ResponseEntity<ApiResponse<Void>> updateActiveStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.updateUserOnlineStatus(userPrincipal.getId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/conversation/{partnerPublicId}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String partnerPublicId) {
        User partner = userRepository.findByPublicId(partnerPublicId)
                .orElseThrow(() -> new IllegalArgumentException("대화 상대 정보를 찾을 수 없습니다."));
        messageService.deleteConversation(getAuthenticatedUser(userPrincipal), partner.getId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long messageId) {
        messageService.deleteMessage(messageId, getAuthenticatedUser(userPrincipal));
        return ApiResponse.success(null);
    }

    @PatchMapping("/{messageId}/edit")
    public ResponseEntity<ApiResponse<Void>> editMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long messageId,
            @RequestBody java.util.Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) throw new IllegalArgumentException("수정할 내용을 입력해주세요.");
        messageService.editMessage(messageId, getAuthenticatedUser(userPrincipal), content);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{messageId}/read")
    public ResponseEntity<ApiResponse<Void>> readMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long messageId) {
        messageService.readMessage(messageId, getAuthenticatedUser(userPrincipal));
        return ApiResponse.success(null);
    }

    @PostMapping("/{messageId}/report")
    public ResponseEntity<ApiResponse<Void>> reportMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long messageId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "부적절한 내용의 쪽지";
        messageService.reportMessage(messageId, getAuthenticatedUser(userPrincipal));
        com.habidue.app.dto.board.ReportRequestDto reportDto = new com.habidue.app.dto.board.ReportRequestDto();
        reportDto.setTargetId(messageId);
        reportDto.setTargetType(com.habidue.app.domain.board.ReportTargetType.MESSAGE);
        reportDto.setReason(reason);
        boardReportService.report(reportDto);
        return ApiResponse.success(null);
    }

    @PostMapping("/block/{blockedPublicId}")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String blockedPublicId) {
        User blockedUser = userRepository.findByPublicId(blockedPublicId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        messageService.blockUser(getAuthenticatedUser(userPrincipal), blockedUser.getId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/block/{blockedPublicId}")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String blockedPublicId) {
        User blockedUser = userRepository.findByPublicId(blockedPublicId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        messageService.unblockUser(getAuthenticatedUser(userPrincipal), blockedUser.getId());
        return ApiResponse.success(null);
    }

    @GetMapping("/block/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBlockedUsers(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(messageService.getBlockedUsersWithDetails(getAuthenticatedUser(userPrincipal)));
    }
}
