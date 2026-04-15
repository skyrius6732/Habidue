package com.habidue.app.service.barter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habidue.app.domain.barter.*;
import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.user.ExpReason;
import com.habidue.app.domain.user.KarmaReason;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.barter.TradeProposalResponseDto;
import com.habidue.app.dto.barter.TradeScheduleRequestDto;
import com.habidue.app.repository.barter.TradeProposalRepository;
import com.habidue.app.repository.board.PostRepository;
import com.habidue.app.repository.message.MessageRepository;
import com.habidue.app.repository.message.UserBlockRepository;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.service.notification.NotificationService;
import com.habidue.app.domain.notification.NotificationType;
import com.habidue.app.service.user.ExpService;
import com.habidue.app.service.user.KarmaService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BarterService {

    private final PostRepository postRepository;
    private final TradeProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final KarmaService karmaService;
    private final NotificationService notificationService;
    private final ExpService expService;
    private final MessageRepository messageRepository;
    private final UserBlockRepository userBlockRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    /**
     * 물물교환 제안하기
     * 비용: 카르마 0.1P (1단위) 차감
     */
    public TradeProposalResponseDto proposeTrade(Long proposerId, Long barterPostId, Long offeredPostId, String message) {
        User proposer = userRepository.findById(proposerId).orElseThrow();
        Post barterPost = postRepository.findById(barterPostId).orElseThrow();
        Post offeredPost = postRepository.findById(offeredPostId).orElseThrow();
        User receiver = barterPost.getAuthor();

        if (barterPost.getAuthor().getId().equals(proposerId)) {
            throw new IllegalArgumentException("자신의 물건에는 제안할 수 없습니다.");
        }

        // [차단 체크] 쪽지함에서 차단된 대화방이면 불가
        if (messageRepository.existsRestrictedMessageBetweenUsers(proposer, receiver)) {
            throw new IllegalStateException("상대방과의 거래가 제한되었습니다.");
        }

        // [차단 체크] 차단된 사용자 관리에 있으면 불가
        if (userBlockRepository.existsByBlockerAndBlocked(proposer, receiver) ||
            userBlockRepository.existsByBlockerAndBlocked(receiver, proposer)) {
            throw new IllegalStateException("상대방에게 차단되어 물물교환이 불가합니다.");
        }

        if (proposalRepository.existsByProposerAndBarterPostAndStatusIn(proposer, barterPost, List.of(ProposalStatus.PROPOSED, ProposalStatus.NEGOTIATING, ProposalStatus.ACCEPTED))) {
            throw new IllegalStateException("이미 진행 중인 제안이 있습니다.");
        }

        if (proposer.getKarmaPoint() < 800) {
            throw new IllegalStateException("신뢰 점수(Karma)가 낮아 제안이 제한되었습니다.");
        }

        TradeProposal proposal = TradeProposal.builder()
                .proposer(proposer)
                .receiver(barterPost.getAuthor())
                .barterPost(barterPost)
                .offeredPost(offeredPost)
                .message(message)
                .build();

        TradeProposal saved = proposalRepository.save(proposal);

        // 비용 차감 (0.1P)
        karmaService.manualAdjustKarmaRaw(proposerId, -1, KarmaReason.BARTER_PROPOSAL, "물물교환 제안 비용 소모", null, true);

        // [하이브리드] 물물교환 요청 안내 시스템 메시지 저장 (먼저 표시)
        String noticeContent = String.format("📢 [안내] %s건으로 물물교환 요청이 왔습니다.", offeredPost.getTitle());
        com.habidue.app.domain.message.Message noticeMessage = com.habidue.app.domain.message.Message.builder()
                .sender(proposer)
                .receiver(receiver)
                .content(noticeContent)
                .tradeProposalId(saved.getId())
                .isRead(false)
                .isSystem(true)
                .build();
        messageRepository.save(noticeMessage);

        // [하이브리드] 제안 메시지를 사용자 메시지로 저장 (사용자가 보낸 것처럼 표시)
        if (message != null && !message.trim().isEmpty()) {
            com.habidue.app.domain.message.Message proposalMessage = com.habidue.app.domain.message.Message.builder()
                    .sender(proposer)
                    .receiver(receiver)
                    .content(message)
                    .tradeProposalId(saved.getId())
                    .isRead(false)
                    .isSystem(false)
                    .build();
            messageRepository.save(proposalMessage);
        }

        // 알림 발송 (마이페이지 > 물물교환으로 이동)
        notificationService.send(receiver, NotificationType.BARTER_PROPOSAL,
                String.format("🤝 '%s'님이 '%s' 물건에 교환 제안을 보냈습니다.", proposer.getNickname(), barterPost.getTitle()),
                saved.getId(), null);

        // JOIN FETCH로 다시 조회해서 모든 관계 로드 (세션 내에서 DTO 변환)
        TradeProposal fetchedProposal = proposalRepository.findByIdWithFetch(saved.getId()).orElseThrow();
        return TradeProposalResponseDto.from(fetchedProposal);
    }

    /**
     * 제안 수락/거절/협의
     */
    public void respondToProposal(Long proposalId, ProposalStatus newStatus, Long userId) {
        TradeProposal proposal = proposalRepository.findById(proposalId).orElseThrow();

        // 권한 체크: PROPOSED 상태에서는 수신자만, REJECTED는 누구나 가능
        if (newStatus == ProposalStatus.REJECTED) {
            // 거래거절: 제안자 또는 수신자 모두 가능
            if (!proposal.getProposer().getId().equals(userId) && !proposal.getReceiver().getId().equals(userId)) {
                throw new IllegalArgumentException("권한이 없습니다.");
            }
        } else {
            // 제안 수락: 수신자만 가능
            if (!proposal.getReceiver().getId().equals(userId)) {
                throw new IllegalArgumentException("권한이 없습니다.");
            }
        }

        proposal.setStatus(newStatus);

        // 제안에 대한 응답 알림
        NotificationType notificationType = newStatus == ProposalStatus.ACCEPTED
                ? NotificationType.PROPOSAL_ACCEPTED
                : NotificationType.PROPOSAL_REJECTED;
        String statusName = newStatus.getDescription();
        notificationService.send(proposal.getProposer(), notificationType,
                String.format("📢 '%s' 물건에 대한 제안이 [%s] 되었습니다.", proposal.getBarterPost().getTitle(), statusName),
                proposal.getId(), null);

        if (newStatus == ProposalStatus.ACCEPTED) {
            proposal.getBarterPost().setBarterStatus(BarterStatus.RESERVED);
            proposal.getOfferedPost().setBarterStatus(BarterStatus.RESERVED);
        } else if (newStatus == ProposalStatus.REJECTED) {
            // 거절 시 양쪽 물품을 다시 TRADING 상태로 변경
            proposal.getBarterPost().setBarterStatus(BarterStatus.TRADING);
            proposal.getOfferedPost().setBarterStatus(BarterStatus.TRADING);
        }
    }

    /**
     * 거래 완료 처리 (양쪽 모두 누를 때까지 대기)
     * 변경사항:
     * - 카르마 회수 안 함 (제안 비용으로 충분)
     * - 경험치 +100EXP 추가
     * - 양쪽이 모두 누를 때만 최종 완료
     */
    public TradeProposalResponseDto completeTrade(Long proposalId, Long userId) {
        // 1. 최신 데이터를 가져옴 (Fetch Join 활용)
        TradeProposal proposal = proposalRepository.findByIdWithFetch(proposalId).orElseThrow();

        // [시니어 핵심 조치] 영속성 컨텍스트에 캐싱된 데이터가 있을 수 있으므로 DB와 강제 동기화
        // 이를 통해 상대방이 먼저 저장한 완료 시간(CompletedAt)을 확실하게 읽어옴
        entityManager.refresh(proposal);

        // 제안자 또는 수신자만 완료 가능
        if (!proposal.getProposer().getId().equals(userId) && !proposal.getReceiver().getId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        // 이미 완료된 상태면 중복 처리 방지
        if (proposal.getStatus() == ProposalStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 거래입니다.");
        }

        // AGREED 또는 NEGOTIATING 상태에서만 완료 가능
        if (proposal.getStatus() != ProposalStatus.ACCEPTED && proposal.getStatus() != ProposalStatus.NEGOTIATING) {
            throw new IllegalStateException("거래 조건이 수락되지 않았습니다.");
        }

        // 디버깅: 현재 상태 확인
        log.info("📝 completeTrade 호출 - proposalId: {}, userId: {}, proposer: {}, receiver: {}",
                proposalId, userId, proposal.getProposer().getId(), proposal.getReceiver().getId());
        log.info("📊 DB에서 읽어온 값 - proposerCompletedAt: {}, receiverCompletedAt: {}",
                proposal.getProposerCompletedAt(), proposal.getReceiverCompletedAt());

        // 내 역할에 맞는 완료 시간 기록
        if (proposal.getProposer().getId().equals(userId)) {
            log.info("✅ 제안자 완료 시간 기록");
            proposal.setProposerCompletedAt(LocalDateTime.now());
        } else {
            log.info("✅ 수신자 완료 시간 기록");
            proposal.setReceiverCompletedAt(LocalDateTime.now());
        }

        // [시니어 조치] 즉시 DB에 반영하여 상대방의 다음 요청이나 상태 체크에서 최신 값을 보장
        proposalRepository.saveAndFlush(proposal);
        log.info("💾 저장 후 - proposerCompletedAt: {}, receiverCompletedAt: {}",
                proposal.getProposerCompletedAt(), proposal.getReceiverCompletedAt());

        // 양쪽 모두 완료했는지 다시 확인 (이제 양쪽 값이 확실히 존재함)
        if (proposal.getProposerCompletedAt() != null && proposal.getReceiverCompletedAt() != null) {
            log.info("🎊 양쪽 모두 완료 확인 - 최종 거래 완료 처리 시작");
            
            proposal.setFinalCompletedAt(LocalDateTime.now());
            proposal.setStatus(ProposalStatus.COMPLETED);

            // 양쪽 물품 상태 변경
            proposal.getBarterPost().setBarterStatus(BarterStatus.COMPLETED);
            proposal.getOfferedPost().setBarterStatus(BarterStatus.COMPLETED);

            proposalRepository.saveAndFlush(proposal);

            // 경험치 추가 (거래 성공) - 양쪽 모두 100EXP 획득
            expService.grantExp(proposal.getProposer().getId(), ExpReason.BARTER_COMPLETED, "물물교환 거래 완료");
            expService.grantExp(proposal.getReceiver().getId(), ExpReason.BARTER_COMPLETED, "물물교환 거래 완료");

            // 양쪽에게 완료 알림
            notificationService.send(proposal.getProposer(), NotificationType.BARTER_COMPLETED,
                    "🎉 물물교환 거래가 완료되었습니다!",
                    proposal.getId(), null);
            notificationService.send(proposal.getReceiver(), NotificationType.BARTER_COMPLETED,
                    "🎉 물물교환 거래가 완료되었습니다!",
                    proposal.getId(), null);
        } else {
            // 한쪽만 완료 → 상대방에게 알림
            User counterparty = proposal.getProposer().getId().equals(userId) ? proposal.getReceiver() : proposal.getProposer();
            notificationService.send(counterparty, NotificationType.BARTER_COMPLETED,
                    "⏳ 상대방이 거래 완료를 표시했습니다. 거래 완료 처리를 진행해 주세요.",
                    proposal.getId(), null);
        }

        return TradeProposalResponseDto.from(proposal);
    }

    /**
     * 거래 취소 (ACCEPTED/NEGOTIATING 상태에서만 가능)
     */
    public TradeProposalResponseDto cancelTrade(Long proposalId, Long userId) {
        TradeProposal proposal = proposalRepository.findById(proposalId).orElseThrow();

        // 제안자 또는 수신자만 취소 가능
        if (!proposal.getProposer().getId().equals(userId) && !proposal.getReceiver().getId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        // ACCEPTED, NEGOTIATING 상태일 때만 취소 가능
        if (proposal.getStatus() != ProposalStatus.ACCEPTED && proposal.getStatus() != ProposalStatus.NEGOTIATING) {
            throw new IllegalStateException("현재 상태에서는 취소할 수 없습니다.");
        }

        // 취소 처리
        proposal.setStatus(ProposalStatus.CANCELLED);
        proposal.getBarterPost().setBarterStatus(BarterStatus.TRADING);
        proposal.getOfferedPost().setBarterStatus(BarterStatus.TRADING);

        // 상대방에게 알림
        User counterparty = proposal.getProposer().getId().equals(userId) ? proposal.getReceiver() : proposal.getProposer();
        notificationService.send(counterparty, NotificationType.TRADE_CANCELLED,
                "🚫 상대방이 거래를 취소했습니다.",
                proposal.getId(), null);

        TradeProposal fetchedProposal = proposalRepository.findByIdWithFetch(proposalId).orElseThrow();
        return TradeProposalResponseDto.from(fetchedProposal);
    }

    @Transactional(readOnly = true)
    public List<com.habidue.app.dto.barter.TradeProposalResponseDto> getMyProposals(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return proposalRepository.findAllByUser(user).stream()
                .map(proposal -> {
                    TradeProposalResponseDto dto = TradeProposalResponseDto.from(proposal);
                    // 상대방이 나를 차단했는지 확인
                    com.habidue.app.domain.user.User opponent = proposal.getProposer().getId().equals(userId) ? proposal.getReceiver() : proposal.getProposer();
                    boolean blockedByPartner = userBlockRepository.existsByBlockerAndBlocked(opponent, user);
                    dto.setBlockedByPartner(blockedByPartner);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 추가 질문하기 (최대 3회)
     * 비용: 카르마 0.1P (1단위) 차감
     */
    public void askQuestion(Long proposalId, String content, Long userId) {
        TradeProposal proposal = proposalRepository.findById(proposalId).orElseThrow();
        User sender = userRepository.findById(userId).orElseThrow();

        // 제안자 또는 수신자만 질문 가능
        if (!proposal.getProposer().getId().equals(userId) && !proposal.getReceiver().getId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        if (proposal.getQuestionCount() >= 3) {
            throw new IllegalStateException("추가 질문은 최대 3회까지만 가능합니다.");
        }

        proposal.incrementQuestionCount();

        // 비용 차감
        karmaService.manualAdjustKarmaRaw(userId, -1, KarmaReason.MESSAGE_SENT, "물물교환 추가 질문 비용 소모", null, true);

        // [하이브리드] Message 엔티티로 저장 (시스템 메시지 형식)
        User receiver = proposal.getProposer().getId().equals(userId) ? proposal.getReceiver() : proposal.getProposer();
        com.habidue.app.domain.message.Message message = com.habidue.app.domain.message.Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .tradeProposalId(proposalId)
                .isRead(false)
                .isSystem(true)
                .build();
        messageRepository.save(message);

        // 상대방에게 알림
        notificationService.send(receiver, NotificationType.SYSTEM,
                String.format("💬 거래 제안의 새로운 질문이 도착했습니다"),
                proposalId, null);
    }

    /**
     * 거래 제안 관련 질문/답변 조회
     */
    public java.util.List<com.habidue.app.dto.message.MessageResponseDto> getProposalQuestions(Long proposalId) {
        java.util.List<com.habidue.app.domain.message.Message> messages = messageRepository.findByTradeProposalId(proposalId);
        return messages.stream()
                .map(com.habidue.app.dto.message.MessageResponseDto::from)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 거래 조건 제시
     * 제안자 또는 수신자가 거래 조건 제시
     */
    public TradeProposalResponseDto proposeSchedule(Long proposalId, TradeScheduleRequestDto scheduleDto, Long userId) {
        TradeProposal proposal = proposalRepository.findById(proposalId).orElseThrow();

        // 제안자 또는 수신자만 조건 제시 가능
        if (!proposal.getProposer().getId().equals(userId) && !proposal.getReceiver().getId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        try {
            String scheduleJson = objectMapper.writeValueAsString(scheduleDto);

            if (proposal.getProposer().getId().equals(userId)) {
                // 제안자가 제시
                proposal.setProposerScheduleJson(scheduleJson);
                proposal.setLastScheduleSetBy("PROPOSER");
            } else {
                // 수신자가 제시
                proposal.setReceiverScheduleJson(scheduleJson);
                proposal.setLastScheduleSetBy("RECEIVER");
            }

            proposal.setStatus(ProposalStatus.NEGOTIATING);
            proposal.setNegotiationRound(proposal.getNegotiationRound() != null ? proposal.getNegotiationRound() + 1 : 1);

            // 양쪽 물품 상태를 RESERVED로 설정
            proposal.getBarterPost().setBarterStatus(BarterStatus.RESERVED);
            proposal.getOfferedPost().setBarterStatus(BarterStatus.RESERVED);

            // 상대방에게 알림
            User counterparty = proposal.getProposer().getId().equals(userId) ? proposal.getReceiver() : proposal.getProposer();
            notificationService.send(counterparty, NotificationType.TRADE_CONDITION_PROPOSED,
                    String.format("⚙️ '%s'님이 거래 조건을 제시했습니다. 확인해주세요.",
                    proposal.getProposer().getId().equals(userId) ? proposal.getProposer().getNickname() : proposal.getReceiver().getNickname()),
                    proposal.getId(), null);

            TradeProposal fetchedProposal = proposalRepository.findByIdWithFetch(proposal.getId()).orElseThrow();
            return TradeProposalResponseDto.from(fetchedProposal);
        } catch (Exception e) {
            throw new RuntimeException("거래 조건 저장 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 거래 조건 수락
     */
    public TradeProposalResponseDto acceptSchedule(Long proposalId, Long userId) {
        TradeProposal proposal = proposalRepository.findById(proposalId).orElseThrow();

        // 제안자 또는 수신자만 수락 가능
        if (!proposal.getProposer().getId().equals(userId) && !proposal.getReceiver().getId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        // 최신 제시된 조건을 최종 합의로 설정
        if (proposal.getProposer().getId().equals(userId)) {
            // 제안자가 수락 (수신자의 조건 또는 제안자의 조건 수락)
            if (proposal.getReceiverScheduleJson() != null) {
                proposal.setAgreedScheduleJson(proposal.getReceiverScheduleJson());
            } else {
                proposal.setAgreedScheduleJson(proposal.getProposerScheduleJson());
            }
        } else {
            // 수신자가 수락 (제안자의 조건 수락)
            proposal.setAgreedScheduleJson(proposal.getProposerScheduleJson());
        }

        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.getBarterPost().setBarterStatus(BarterStatus.RESERVED);
        proposal.getOfferedPost().setBarterStatus(BarterStatus.RESERVED);

        // 상대방에게 알림
        User counterparty = proposal.getProposer().getId().equals(userId) ? proposal.getReceiver() : proposal.getProposer();
        notificationService.send(counterparty, NotificationType.TRADE_CONDITION_ACCEPTED,
                String.format("✅ '%s'님이 거래 조건을 수락했습니다!",
                proposal.getProposer().getId().equals(userId) ? proposal.getProposer().getNickname() : proposal.getReceiver().getNickname()),
                proposal.getId(), null);

        TradeProposal fetchedProposal = proposalRepository.findByIdWithFetch(proposal.getId()).orElseThrow();
        return TradeProposalResponseDto.from(fetchedProposal);
    }

    /**
     * 거래 조건 반박 (재협상)
     */
    public TradeProposalResponseDto counterSchedule(Long proposalId, TradeScheduleRequestDto scheduleDto, Long userId) {
        TradeProposal proposal = proposalRepository.findById(proposalId).orElseThrow();

        // 제안자 또는 수신자만 반박 가능
        if (!proposal.getProposer().getId().equals(userId) && !proposal.getReceiver().getId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        // 최대 협상 라운드 체크 (최대 10라운드로 제한)
        if (proposal.getNegotiationRound() != null && proposal.getNegotiationRound() >= 10) {
            throw new IllegalStateException("협상은 최대 10회까지만 가능합니다.");
        }

        try {
            String scheduleJson = objectMapper.writeValueAsString(scheduleDto);

            if (proposal.getProposer().getId().equals(userId)) {
                proposal.setProposerScheduleJson(scheduleJson);
                proposal.setLastScheduleSetBy("PROPOSER");
            } else {
                proposal.setReceiverScheduleJson(scheduleJson);
                proposal.setLastScheduleSetBy("RECEIVER");
            }

            proposal.setStatus(ProposalStatus.NEGOTIATING);
            proposal.setNegotiationRound(proposal.getNegotiationRound() != null ? proposal.getNegotiationRound() + 1 : 1);

            // 양쪽 물품 상태를 RESERVED로 설정
            proposal.getBarterPost().setBarterStatus(BarterStatus.RESERVED);
            proposal.getOfferedPost().setBarterStatus(BarterStatus.RESERVED);

            // 상대방에게 알림
            User counterparty = proposal.getProposer().getId().equals(userId) ? proposal.getReceiver() : proposal.getProposer();
            notificationService.send(counterparty, NotificationType.TRADE_CONDITION_COUNTER,
                    String.format("♻️ '%s'님이 거래 조건을 다시 제시했습니다. 확인해주세요.",
                    proposal.getProposer().getId().equals(userId) ? proposal.getProposer().getNickname() : proposal.getReceiver().getNickname()),
                    proposal.getId(), null);

            TradeProposal fetchedProposal = proposalRepository.findByIdWithFetch(proposal.getId()).orElseThrow();
            return TradeProposalResponseDto.from(fetchedProposal);
        } catch (Exception e) {
            throw new RuntimeException("거래 조건 반박 중 오류 발생: " + e.getMessage());
        }
    }
}
