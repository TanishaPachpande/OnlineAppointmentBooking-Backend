package com.medibook.schedule.repository;

import com.medibook.schedule.entity.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    // Used to prevent creating the same slot twice
    boolean existsByProviderIdAndDateAndStartTime(Long providerId, LocalDate date, LocalTime startTime);

    List<AvailabilitySlot> findByProviderId(Long providerId);

    List<AvailabilitySlot> findByProviderIdAndDate(Long providerId, LocalDate date);

    List<AvailabilitySlot> findByProviderIdAndDateAndIsBookedFalseAndIsBlockedFalse(Long providerId, LocalDate date);

    List<AvailabilitySlot> findByDateBetween(LocalDate startDate, LocalDate endDate);

    Long countByProviderIdAndIsBookedFalseAndIsBlockedFalse(Long providerId);

    // ADDED: bulk-block all unbooked slots from fully past dates in one query (used by scheduler)
    @Modifying
    @Query("UPDATE AvailabilitySlot s SET s.isBlocked = true " +
            "WHERE s.date < :today AND s.isBooked = false AND s.isBlocked = false")
    int blockAllExpiredUnbookedSlots(@Param("today") LocalDate today);

    // ADDED: find today's unbooked slots whose startTime has already passed (used by scheduler)
    @Query("SELECT s FROM AvailabilitySlot s " +
            "WHERE s.date = :today AND s.startTime < :now " +
            "AND s.isBooked = false AND s.isBlocked = false")
    List<AvailabilitySlot> findTodayExpiredUnbookedSlots(
            @Param("today") LocalDate today,
            @Param("now") LocalTime now);
}
