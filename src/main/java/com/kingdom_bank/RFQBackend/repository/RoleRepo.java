package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.Role;
import com.kingdom_bank.RFQBackend.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepo extends JpaRepository<Role, Integer> {
    List<Role> findByStatus_StatusIdInOrderByDateAddedDesc(List<Integer> ids);
    List<Role> findByRoleNameAndStatusIn(String roleName, List<Status> statusList);
}
