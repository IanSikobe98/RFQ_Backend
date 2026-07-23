package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.Branch;
import com.kingdom_bank.RFQBackend.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepo extends JpaRepository<Branch, Integer> {

    Optional<Branch> findByBranchNameAndStatusId(String branchName, Status statusId);
    Optional<Branch> findByBranchCodeAndStatusId(String branchName, Status statusId);
    List<Branch> findByStatusId_StatusIdInOrderByDateCreatedDesc(List<Integer> statusId);
}
