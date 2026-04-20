package com.medibook.review.repository;

import com.medibook.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProviderId(Long providerId);

    List<Review> findByPatientId(Long patientId);

    Optional<Review> findByAppointmentId(Long appointmentId);

    Long countByProviderId(Long providerId);
}