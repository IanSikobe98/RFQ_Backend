package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.Status;
import com.kingdom_bank.RFQBackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Integer> {
    User findDistinctByUsernameEqualsIgnoreCaseAndStatusIn(String username, List<Status> statusList);
    Optional<User> findByUserIdAndStatus(Integer id, Status status);
//    User findDistinctByUsernameEqualsIgnoreCaseAndStatusInAndUser_StatusInAndUser_Org_StatusIn
}
