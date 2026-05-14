package com.medibook.record.controller;

import com.medibook.record.dto.ApiResponseDto;
import com.medibook.record.dto.MedicalRecordRequestDto;
import com.medibook.record.dto.MedicalRecordResponseDto;
import com.medibook.record.service.MedicalRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/records")
@Tag(name = "Medical Record Controller", description = "APIs for patient medical records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    @PostMapping
    public ResponseEntity<MedicalRecordResponseDto> createRecord(@Valid @RequestBody MedicalRecordRequestDto requestDto) {
        log.info("API CALL: Create medical record for appointmentId={}", requestDto.getAppointmentId());
        return ResponseEntity.ok(medicalRecordService.createRecord(requestDto));
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<MedicalRecordResponseDto> updateRecord(@PathVariable Long recordId,
                                                                 @Valid @RequestBody MedicalRecordRequestDto requestDto) {
        return ResponseEntity.ok(medicalRecordService.updateRecord(recordId, requestDto));
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<MedicalRecordResponseDto> getRecordById(@PathVariable Long recordId) {
        return ResponseEntity.ok(medicalRecordService.getRecordById(recordId));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<MedicalRecordResponseDto> getRecordByAppointmentId(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(medicalRecordService.getRecordByAppointmentId(appointmentId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalRecordResponseDto>> getRecordsByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(medicalRecordService.getRecordsByPatient(patientId));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<MedicalRecordResponseDto>> getRecordsByProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(medicalRecordService.getRecordsByProvider(providerId));
    }

    @GetMapping("/patient/{patientId}/provider/{providerId}")
    public ResponseEntity<List<MedicalRecordResponseDto>> getRecordsByPatientAndProvider(@PathVariable Long patientId,
                                                                                         @PathVariable Long providerId) {
        return ResponseEntity.ok(medicalRecordService.getRecordsByPatientAndProvider(patientId, providerId));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponseDto> deleteRecord(@PathVariable Long recordId) {
        return ResponseEntity.ok(
                ApiResponseDto.builder()
                        .message(medicalRecordService.deleteRecord(recordId))
                        .build()
        );
    }
}