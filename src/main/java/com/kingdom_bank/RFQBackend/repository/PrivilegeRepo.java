package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.Privilege;
import com.kingdom_bank.RFQBackend.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivilegeRepo extends JpaRepository<Privilege, Integer> {
    Privilege findByPrivilegeIdAndStatus(Integer privilegeId, Status status);
    List<Privilege> findByStatus_StatusIdInOrderByDateAddedDesc(List<Integer> statusId);

}

