package com.medibook.record.service;

import com.medibook.record.dto.MedicalRecordRequestDto;
import com.medibook.record.dto.MedicalRecordResponseDto;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.exception.BusinessException;
import com.medibook.record.exception.ResourceNotFoundException;
import com.medibook.record.repository.MedicalRecordRepository;
import com.medibook.record.service.impl.MedicalRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceImplTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @InjectMocks private MedicalRecordServiceImpl medicalRecordService;

    private MedicalRecord sampleRecord;
    private MedicalRecordRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        sampleRecord = MedicalRecord.builder()
                .recordId(1L).appointmentId(10L).patientId(100L).providerId(200L)
                .visitDate(LocalDate.now()).diagnosis("Hypertension")
                .prescription("Medication A").labTests("Blood test").followUpNotes("Review in 2 weeks")
                .allergies("Penicillin").vitals("BP: 140/90")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        sampleRequest = MedicalRecordRequestDto.builder()
                .appointmentId(10L).patientId(100L).providerId(200L)
                .visitDate(LocalDate.now()).diagnosis("Hypertension")
                .prescription("Medication A").build();
    }

    @Test
    void createRecord_success() {
        when(medicalRecordRepository.findByAppointmentId(10L)).thenReturn(Optional.empty());
        when(medicalRecordRepository.save(any())).thenReturn(sampleRecord);

        MedicalRecordResponseDto result = medicalRecordService.createRecord(sampleRequest);

        assertThat(result.getRecordId()).isEqualTo(1L);
        assertThat(result.getDiagnosis()).isEqualTo("Hypertension");
        verify(medicalRecordRepository).save(any());
    }

    @Test
    void createRecord_duplicateAppointment_throws() {
        when(medicalRecordRepository.findByAppointmentId(10L)).thenReturn(Optional.of(sampleRecord));

        assertThatThrownBy(() -> medicalRecordService.createRecord(sampleRequest))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already exists");
    }

    @Test
    void updateRecord_success() {
        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(sampleRecord));
        when(medicalRecordRepository.save(any())).thenReturn(sampleRecord);

        MedicalRecordResponseDto result = medicalRecordService.updateRecord(1L, sampleRequest);

        assertThat(result).isNotNull();
        verify(medicalRecordRepository).save(any());
    }

    @Test
    void updateRecord_notFound_throws() {
        when(medicalRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicalRecordService.updateRecord(99L, sampleRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRecordById_found() {
        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(sampleRecord));
        assertThat(medicalRecordService.getRecordById(1L).getPatientId()).isEqualTo(100L);
    }

    @Test
    void getRecordById_notFound_throws() {
        when(medicalRecordRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> medicalRecordService.getRecordById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRecordByAppointmentId_found() {
        when(medicalRecordRepository.findByAppointmentId(10L)).thenReturn(Optional.of(sampleRecord));
        assertThat(medicalRecordService.getRecordByAppointmentId(10L).getAppointmentId()).isEqualTo(10L);
    }

    @Test
    void getRecordByAppointmentId_notFound_throws() {
        when(medicalRecordRepository.findByAppointmentId(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> medicalRecordService.getRecordByAppointmentId(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRecordsByPatient_returnsList() {
        when(medicalRecordRepository.findByPatientId(100L)).thenReturn(List.of(sampleRecord));
        assertThat(medicalRecordService.getRecordsByPatient(100L)).hasSize(1);
    }

    @Test
    void getRecordsByProvider_returnsList() {
        when(medicalRecordRepository.findByProviderId(200L)).thenReturn(List.of(sampleRecord));
        assertThat(medicalRecordService.getRecordsByProvider(200L)).hasSize(1);
    }

    @Test
    void getRecordsByPatientAndProvider_returnsList() {
        when(medicalRecordRepository.findByPatientIdAndProviderId(100L, 200L))
                .thenReturn(List.of(sampleRecord));
        assertThat(medicalRecordService.getRecordsByPatientAndProvider(100L, 200L)).hasSize(1);
    }

    @Test
    void deleteRecord_success() {
        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(sampleRecord));
        doNothing().when(medicalRecordRepository).delete(sampleRecord);

        assertThat(medicalRecordService.deleteRecord(1L)).contains("deleted");
        verify(medicalRecordRepository).delete(sampleRecord);
    }

    @Test
    void deleteRecord_notFound_throws() {
        when(medicalRecordRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> medicalRecordService.deleteRecord(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
