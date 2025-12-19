package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.ApprovedDeals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovedDealsRepo extends JpaRepository<ApprovedDeals, Integer> {
}
