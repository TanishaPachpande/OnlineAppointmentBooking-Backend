package com.medibook.record.service.impl;

import com.medibook.record.dto.MedicalRecordRequestDto;
import com.medibook.record.dto.MedicalRecordResponseDto;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.exception.BusinessException;
import com.medibook.record.exception.ResourceNotFoundException;
import com.medibook.record.repository.MedicalRecordRepository;
import com.medibook.record.service.MedicalRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    public MedicalRecordServiceImpl(MedicalRecordRepository medicalRecordRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
    }

    @Override
    public MedicalRecordResponseDto createRecord(MedicalRecordRequestDto requestDto) {
        log.info("Creating medical record for appointmentId={}, patientId={}",
                requestDto.getAppointmentId(), requestDto.getPatientId());

        medicalRecordRepository.findByAppointmentId(requestDto.getAppointmentId()).ifPresent(existing -> {
            log.warn("Medical record already exists for appointmentId={}", requestDto.getAppointmentId());
            throw new BusinessException("Medical record already exists for this appointment");
        });

        MedicalRecord record = MedicalRecord.builder()
                .appointmentId(requestDto.getAppointmentId())
                .patientId(requestDto.getPatientId())
                .providerId(requestDto.getProviderId())
                .visitDate(requestDto.getVisitDate())
                .diagnosis(requestDto.getDiagnosis())
                .prescription(requestDto.getPrescription())
                .labTests(requestDto.getLabTests())
                .followUpNotes(requestDto.getFollowUpNotes())
                .allergies(requestDto.getAllergies())
                .vitals(requestDto.getVitals())
                .build();

        MedicalRecord saved = medicalRecordRepository.save(record);
        log.info("Medical record saved successfully with recordId={}", saved.getRecordId());

        return mapToResponse(saved);
    }

    @Override
    public MedicalRecordResponseDto updateRecord(Long recordId, MedicalRecordRequestDto requestDto) {
        log.info("Updating medical record with recordId={}", recordId);

        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found with id: " + recordId));

        record.setAppointmentId(requestDto.getAppointmentId());
        record.setPatientId(requestDto.getPatientId());
        record.setProviderId(requestDto.getProviderId());
        record.setVisitDate(requestDto.getVisitDate());
        record.setDiagnosis(requestDto.getDiagnosis());
        record.setPrescription(requestDto.getPrescription());
        record.setLabTests(requestDto.getLabTests());
        record.setFollowUpNotes(requestDto.getFollowUpNotes());
        record.setAllergies(requestDto.getAllergies());
        record.setVitals(requestDto.getVitals());

        MedicalRecord saved = medicalRecordRepository.save(record);
        log.info("Medical record updated successfully with recordId={}", saved.getRecordId());

        return mapToResponse(saved);
    }

    @Override
    public MedicalRecordResponseDto getRecordById(Long recordId) {
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found with id: " + recordId));
        return mapToResponse(record);
    }

    @Override
    public MedicalRecordResponseDto getRecordByAppointmentId(Long appointmentId) {
        MedicalRecord record = medicalRecordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found for appointmentId: " + appointmentId));
        return mapToResponse(record);
    }

    @Override
    public List<MedicalRecordResponseDto> getRecordsByPatient(Long patientId) {
        return medicalRecordRepository.findByPatientId(patientId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<MedicalRecordResponseDto> getRecordsByProvider(Long providerId) {
        return medicalRecordRepository.findByProviderId(providerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<MedicalRecordResponseDto> getRecordsByPatientAndProvider(Long patientId, Long providerId) {
        return medicalRecordRepository.findByPatientIdAndProviderId(patientId, providerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public String deleteRecord(Long recordId) {
        log.info("Deleting medical record with recordId={}", recordId);

        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found with id: " + recordId));

        medicalRecordRepository.delete(record);
        log.info("Medical record deleted successfully with recordId={}", recordId);

        return "Medical record deleted successfully";
    }

    private MedicalRecordResponseDto mapToResponse(MedicalRecord record) {
        return MedicalRecordResponseDto.builder()
                .recordId(record.getRecordId())
                .appointmentId(record.getAppointmentId())
                .patientId(record.getPatientId())
                .providerId(record.getProviderId())
                .visitDate(record.getVisitDate())
                .diagnosis(record.getDiagnosis())
                .prescription(record.getPrescription())
                .labTests(record.getLabTests())
                .followUpNotes(record.getFollowUpNotes())
                .allergies(record.getAllergies())
                .vitals(record.getVitals())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}