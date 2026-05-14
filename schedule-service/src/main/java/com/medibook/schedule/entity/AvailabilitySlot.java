package com.medibook.schedule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "availability_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long slotId;

    @Column(nullable = false)
    private Long providerId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private Boolean isBooked;

    @Column(nullable = false)
    private Boolean isBlocked;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceType recurrence;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.isBooked == null) {
            this.isBooked = false;
        }
        if (this.isBlocked == null) {
            this.isBlocked = false;
        }
        if (this.recurrence == null) {
            this.recurrence = RecurrenceType.NONE;
        }
    }
}