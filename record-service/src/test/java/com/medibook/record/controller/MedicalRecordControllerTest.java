package com.medibook.record.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medibook.record.dto.MedicalRecordRequestDto;
import com.medibook.record.dto.MedicalRecordResponseDto;
import com.medibook.record.exception.BusinessException;
import com.medibook.record.exception.ResourceNotFoundException;
import com.medibook.record.service.MedicalRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MedicalRecordControllerTest {

    @Mock private MedicalRecordService medicalRecordService;
    @InjectMocks private MedicalRecordController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MedicalRecordResponseDto sampleResponse;
    private MedicalRecordRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = MedicalRecordResponseDto.builder()
                .recordId(1L).appointmentId(10L).patientId(100L).providerId(200L)
                .visitDate(LocalDate.now()).diagnosis("Hypertension")
                .prescription("Medication A").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = MedicalRecordRequestDto.builder()
                .appointmentId(10L).patientId(100L).providerId(200L)
                .visitDate(LocalDate.now()).diagnosis("Hypertension")
                .prescription("Medication A").build();
    }

    @Test
    void createRecord_returns200() throws Exception {
        when(medicalRecordService.createRecord(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId").value(1L))
                .andExpect(jsonPath("$.diagnosis").value("Hypertension"));

        verify(medicalRecordService).createRecord(any());
    }

    @Test
    void updateRecord_returns200() throws Exception {
        when(medicalRecordService.updateRecord(eq(1L), any())).thenReturn(sampleResponse);

        mockMvc.perform(put("/records/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId").value(1L));
    }

    @Test
    void getRecordById_returns200() throws Exception {
        when(medicalRecordService.getRecordById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(100L));
    }

    @Test
    void getRecordByAppointmentId_returns200() throws Exception {
        when(medicalRecordService.getRecordByAppointmentId(10L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/records/appointment/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(10L));
    }

    @Test
    void getRecordsByPatient_returns200() throws Exception {
        when(medicalRecordService.getRecordsByPatient(100L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/records/patient/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getRecordsByProvider_returns200() throws Exception {
        when(medicalRecordService.getRecordsByProvider(200L)).thenReturn(List.of(sampleResponse, sampleResponse));

        mockMvc.perform(get("/records/provider/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getRecordsByPatientAndProvider_returns200() throws Exception {
        when(medicalRecordService.getRecordsByPatientAndProvider(100L, 200L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/records/patient/100/provider/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deleteRecord_returns200() throws Exception {
        when(medicalRecordService.deleteRecord(1L)).thenReturn("Medical record deleted successfully");

        mockMvc.perform(delete("/records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Medical record deleted successfully"));
    }

    @Test
    void getRecordsByPatient_emptyList_returns200() throws Exception {
        when(medicalRecordService.getRecordsByPatient(999L)).thenReturn(List.of());

        mockMvc.perform(get("/records/patient/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
