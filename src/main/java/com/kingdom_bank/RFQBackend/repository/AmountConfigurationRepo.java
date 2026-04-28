package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.AmountConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AmountConfigurationRepo extends JpaRepository<AmountConfiguration,Integer> {
}
