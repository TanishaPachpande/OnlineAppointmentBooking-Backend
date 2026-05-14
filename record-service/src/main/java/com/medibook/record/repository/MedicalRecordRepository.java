package com.medibook.record.repository;

import com.medibook.record.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    Optional<MedicalRecord> findByAppointmentId(Long appointmentId);

    List<MedicalRecord> findByPatientId(Long patientId);

    List<MedicalRecord> findByProviderId(Long providerId);

    List<MedicalRecord> findByPatientIdAndProviderId(Long patientId, Long providerId);
}