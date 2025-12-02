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
    Optional<User> findByUsernameEqualsIgnoreCaseAndStatusIn(String username, List<Status> status);
    List<User> findByPhoneAndStatusIn(String phone, List<Status> status);
    List<User> findByEmailEqualsIgnoreCaseAndStatusIn(String email, List<Status> status);
    List<User> findByStatus_StatusIdInOrderByDateAddedDesc(List<Integer> statusId);

//    User findDistinctByUsernameEqualsIgnoreCaseAndStatusInAndUser_StatusInAndUser_Org_StatusIn
}
