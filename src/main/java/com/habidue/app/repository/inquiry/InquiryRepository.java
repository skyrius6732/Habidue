package com.habidue.app.repository.inquiry;

import com.habidue.app.domain.inquiry.Inquiry;
import com.habidue.app.domain.inquiry.InquiryStatus;
import com.habidue.app.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // 사용자의 자신의 활성 문의 내역 조회 (삭제하지 않은 것만)
    Page<Inquiry> findByAuthorAndDeletedByAuthorFalseOrderByCreatedAtDesc(User author, Pageable pageable);

    @Query("SELECT i FROM Inquiry i WHERE (:status IS NULL OR i.status = :status) AND " +
           "(:keyword IS NULL OR i.title LIKE %:keyword% OR i.content LIKE %:keyword% OR i.author.nickname LIKE %:keyword%) " +
           "ORDER BY i.createdAt DESC")
    Page<Inquiry> findInquiriesWithSearch(@Param("status") InquiryStatus status, @Param("keyword") String keyword, Pageable pageable);

    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
