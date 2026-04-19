package com.medibook.schedule.repository;

import com.medibook.schedule.entity.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    List<AvailabilitySlot> findByProviderId(Long providerId);

    List<AvailabilitySlot> findByProviderIdAndDate(Long providerId, LocalDate date);

    List<AvailabilitySlot> findByProviderIdAndDateAndIsBookedFalseAndIsBlockedFalse(Long providerId, LocalDate date);

    List<AvailabilitySlot> findByDateBetween(LocalDate startDate, LocalDate endDate);

    Long countByProviderIdAndIsBookedFalseAndIsBlockedFalse(Long providerId);
}