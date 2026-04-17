package com.habidue.app.repository.barter;

import com.habidue.app.domain.barter.TradeProposalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TradeProposalHistoryRepository extends JpaRepository<TradeProposalHistory, Long> {

    @Query("SELECT h FROM TradeProposalHistory h " +
           "WHERE h.proposal.id = :proposalId " +
           "ORDER BY h.round ASC")
    List<TradeProposalHistory> findByProposalIdOrderByRound(@Param("proposalId") Long proposalId);
}
