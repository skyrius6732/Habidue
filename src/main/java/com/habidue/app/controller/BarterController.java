package com.habidue.app.controller;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.barter.ProposalStatus;
import com.habidue.app.dto.barter.TradeProposalRequestDto;
import com.habidue.app.dto.barter.TradeProposalResponseDto;
import com.habidue.app.dto.barter.TradeProposalHistoryResponseDto;
import com.habidue.app.dto.barter.TradeScheduleRequestDto;
import com.habidue.app.dto.barter.TradeCompletionResponseDto;
import com.habidue.app.service.barter.BarterService;
import com.habidue.app.repository.barter.TradeProposalHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/barter")
@RequiredArgsConstructor
public class BarterController {

    private final BarterService barterService;
    private final TradeProposalHistoryRepository historyRepository;

    @PostMapping("/proposals")
    public ResponseEntity<TradeProposalResponseDto> proposeTrade(
            @RequestBody TradeProposalRequestDto requestDto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(barterService.proposeTrade(currentUser.getId(), requestDto.getBarterPostId(),
                requestDto.getOfferedPostId(), requestDto.getMessage(),
                requestDto.getProposerMethod(), requestDto.getProposerLocation(),
                requestDto.getProposerDate(), requestDto.getProposerTime()));
    }

    @GetMapping("/proposals")
    public ResponseEntity<List<TradeProposalResponseDto>> getMyProposals(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(barterService.getMyProposals(currentUser.getId()));
    }

    @GetMapping("/proposals/{proposalId}")
    public ResponseEntity<TradeProposalResponseDto> getProposal(
            @PathVariable Long proposalId) {
        return ResponseEntity.ok(barterService.getProposal(proposalId));
    }

    @PostMapping("/proposals/{proposalId}/respond")
    public ResponseEntity<Void> respondToProposal(
            @PathVariable Long proposalId,
            @RequestParam ProposalStatus status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        barterService.respondToProposal(proposalId, status, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    // [새 방식] 거래 조건 제시
    @PostMapping("/proposals/{proposalId}/schedule/propose")
    public ResponseEntity<TradeProposalResponseDto> proposeSchedule(
            @PathVariable Long proposalId,
            @RequestBody TradeScheduleRequestDto requestDto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(barterService.proposeSchedule(proposalId, requestDto, currentUser.getId()));
    }

    // [새 방식] 거래 조건 수락
    @PostMapping("/proposals/{proposalId}/schedule/accept")
    public ResponseEntity<TradeProposalResponseDto> acceptSchedule(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(barterService.acceptSchedule(proposalId, currentUser.getId()));
    }

    // [새 방식] 거래 조건 반박
    @PostMapping("/proposals/{proposalId}/schedule/counter")
    public ResponseEntity<TradeProposalResponseDto> counterSchedule(
            @PathVariable Long proposalId,
            @RequestBody TradeScheduleRequestDto requestDto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(barterService.counterSchedule(proposalId, requestDto, currentUser.getId()));
    }

    @PostMapping("/proposals/{proposalId}/complete")
    public ResponseEntity<TradeCompletionResponseDto> completeTrade(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(barterService.completeTrade(proposalId, currentUser.getId()));
    }

    @PostMapping("/proposals/{proposalId}/cancel")
    public ResponseEntity<TradeCompletionResponseDto> cancelTrade(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(barterService.cancelTrade(proposalId, currentUser.getId()));
    }

    @PostMapping("/proposals/{proposalId}/questions")
    public ResponseEntity<Void> askQuestion(
            @PathVariable Long proposalId,
            @RequestParam String content,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        barterService.askQuestion(proposalId, content, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/proposals/{proposalId}/questions")
    public ResponseEntity<java.util.List<com.habidue.app.dto.message.MessageResponseDto>> getProposalQuestions(
            @PathVariable Long proposalId) {
        return ResponseEntity.ok(barterService.getProposalQuestions(proposalId));
    }

    @GetMapping("/proposals/{proposalId}/history")
    public ResponseEntity<List<TradeProposalHistoryResponseDto>> getNegotiationHistory(
            @PathVariable Long proposalId) {
        return ResponseEntity.ok(historyRepository.findByProposalIdOrderByRound(proposalId).stream()
                .map(TradeProposalHistoryResponseDto::from)
                .collect(Collectors.toList()));
    }
}
