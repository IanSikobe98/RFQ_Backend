package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.AvailabilityConfiguration;
import com.kingdom_bank.RFQBackend.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilityConfigRepo extends JpaRepository<AvailabilityConfiguration,Long> {
    Optional<AvailabilityConfiguration> findByDayAndStatus(String day, Status status);
    Optional<AvailabilityConfiguration> findByDay(String day);
}
