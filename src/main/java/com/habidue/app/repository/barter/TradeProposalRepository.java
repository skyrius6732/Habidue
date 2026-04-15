package com.habidue.app.repository.barter;

import com.habidue.app.domain.barter.ProposalStatus;
import com.habidue.app.domain.barter.TradeProposal;
import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TradeProposalRepository extends JpaRepository<TradeProposal, Long> {
    
    @Query("SELECT tp FROM TradeProposal tp " +
           "JOIN FETCH tp.proposer " +
           "JOIN FETCH tp.receiver " +
           "JOIN FETCH tp.barterPost " +
           "JOIN FETCH tp.offeredPost " +
           "WHERE tp.proposer = :user OR tp.receiver = :user " +
           "ORDER BY tp.createdAt DESC")
    List<TradeProposal> findAllByUser(@Param("user") User user);

    @Query("SELECT tp FROM TradeProposal tp " +
           "JOIN FETCH tp.proposer " +
           "JOIN FETCH tp.receiver " +
           "JOIN FETCH tp.barterPost " +
           "JOIN FETCH tp.offeredPost " +
           "WHERE tp.id = :id")
    java.util.Optional<TradeProposal> findByIdWithFetch(@Param("id") Long id);

    boolean existsByProposerAndBarterPostAndStatusIn(User proposer, Post barterPost, List<ProposalStatus> statuses);
}
