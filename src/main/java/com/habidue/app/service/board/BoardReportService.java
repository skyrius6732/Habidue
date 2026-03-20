package com.habidue.app.service.board;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.board.Comment;
import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.board.Report;
import com.habidue.app.domain.board.ReportTargetType;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.board.ReportRequestDto;
import com.habidue.app.repository.board.CommentRepository;
import com.habidue.app.repository.board.PostRepository;
import com.habidue.app.repository.board.ReportRepository;
import com.habidue.app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
    }

    public void report(ReportRequestDto requestDto) {
        User reporter = getCurrentUser();

        // [시니어 로직] 중복 신고 검증
        if (reportRepository.existsByReporter_IdAndTargetIdAndTargetType(reporter.getId(), requestDto.getTargetId(), requestDto.getTargetType())) {
            throw new IllegalArgumentException("이미 신고하신 " + (requestDto.getTargetType() == ReportTargetType.POST ? "게시글" : "댓글") + "입니다.");
        }

        // [시니어 로직] 본인 신고 및 상태 검증
        if (requestDto.getTargetType() == ReportTargetType.POST) {
            Post post = postRepository.findById(requestDto.getTargetId())
                    .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
            if ("BLINDED".equalsIgnoreCase(post.getStatus())) {
                throw new IllegalArgumentException("이미 관리자에 의해 조치된 게시글입니다.");
            }
            if (post.getAuthor().getId().equals(reporter.getId())) {
                throw new IllegalArgumentException("자신의 게시글은 신고할 수 없습니다.");
            }
        } else if (requestDto.getTargetType() == ReportTargetType.COMMENT) {
            Comment comment = commentRepository.findById(requestDto.getTargetId())
                    .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));
            if ("BLINDED".equalsIgnoreCase(comment.getStatus())) {
                throw new IllegalArgumentException("이미 관리자에 의해 조치된 댓글입니다.");
            }
            if (comment.getAuthor().getId().equals(reporter.getId())) {
                throw new IllegalArgumentException("자신의 댓글은 신고할 수 없습니다.");
            }
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(requestDto.getTargetType())
                .targetId(requestDto.getTargetId())
                .reason(requestDto.getReason())
                .build();

        reportRepository.save(report);
    }
}
