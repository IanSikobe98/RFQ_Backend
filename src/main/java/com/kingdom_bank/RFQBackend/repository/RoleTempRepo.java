package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.RolesTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleTempRepo extends JpaRepository<RolesTemp,Integer> {
    List<RolesTemp> findByStatus_StatusIdInOrderByDateAddedDesc(List<Integer> statusId);
}
