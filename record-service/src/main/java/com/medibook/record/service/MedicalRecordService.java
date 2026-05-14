package com.medibook.record.service;

import com.medibook.record.dto.MedicalRecordRequestDto;
import com.medibook.record.dto.MedicalRecordResponseDto;

import java.util.List;

public interface MedicalRecordService {

    MedicalRecordResponseDto createRecord(MedicalRecordRequestDto requestDto);

    MedicalRecordResponseDto updateRecord(Long recordId, MedicalRecordRequestDto requestDto);

    MedicalRecordResponseDto getRecordById(Long recordId);

    MedicalRecordResponseDto getRecordByAppointmentId(Long appointmentId);

    List<MedicalRecordResponseDto> getRecordsByPatient(Long patientId);

    List<MedicalRecordResponseDto> getRecordsByProvider(Long providerId);

    List<MedicalRecordResponseDto> getRecordsByPatientAndProvider(Long patientId, Long providerId);

    String deleteRecord(Long recordId);
}