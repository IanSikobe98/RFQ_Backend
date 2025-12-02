package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.UsersTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTempRepo extends JpaRepository<UsersTemp,Integer> {
}
