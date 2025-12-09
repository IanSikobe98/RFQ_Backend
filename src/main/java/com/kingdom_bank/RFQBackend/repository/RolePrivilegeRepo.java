package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.RolePrivilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePrivilegeRepo extends JpaRepository<RolePrivilege, Integer> {
    List<RolePrivilege> findByRole_RoleId(Integer roleId);
    List<RolePrivilege> findByRole_RoleIdIn(List<Integer> roleId);
}
