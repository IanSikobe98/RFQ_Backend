package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.RolePrivilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePrivilegeRepo extends JpaRepository<RolePrivilege, Integer> {
}
