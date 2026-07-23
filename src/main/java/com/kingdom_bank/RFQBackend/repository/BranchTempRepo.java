package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.Branch;
import com.kingdom_bank.RFQBackend.entity.BranchTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchTempRepo extends JpaRepository<BranchTemp,Integer> {

    List<BranchTemp> findByStatus_StatusIdInOrderByDateCreatedDesc(List<Integer> statusId);
}
