package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.Comments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentsRepo extends JpaRepository<Comments, Integer> {
    List<Comments> findByOrder_IdOrderByDateCreatedAsc(Long orderId);
}
