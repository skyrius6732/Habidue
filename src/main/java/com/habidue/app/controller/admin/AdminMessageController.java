package com.habidue.app.controller.admin;

import com.habidue.app.dto.ApiResponse;
import com.habidue.app.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMessageController {

    private final MessageService messageService;

    /**
     * 특정 유저에게 시스템 메시지 발송
     */
    @PostMapping("/system")
    public ResponseEntity<ApiResponse<Void>> sendSystemMessage(@RequestBody Map<String, Object> request) {
        Long receiverId = Long.valueOf(request.get("receiverId").toString());
        String content = (String) request.get("content");

        messageService.sendSystemMessage(receiverId, content);
        return ApiResponse.success(null);
    }
}
