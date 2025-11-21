package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends JpaRepository<User, Integer> {
}
