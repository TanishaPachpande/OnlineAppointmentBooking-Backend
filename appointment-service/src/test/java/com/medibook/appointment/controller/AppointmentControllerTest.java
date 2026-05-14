package com.medibook.appointment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medibook.appointment.dto.*;
import com.medibook.appointment.entity.AppointmentStatus;
import com.medibook.appointment.entity.ConsultationMode;
import com.medibook.appointment.service.AppointmentService;
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
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock private AppointmentService appointmentService;
    @InjectMocks private AppointmentController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AppointmentResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = AppointmentResponseDto.builder()
                .appointmentId(1L).patientId(101L).providerId(1L).slotId(10L)
                .serviceType("General Consultation")
                .appointmentDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30))
                .status(AppointmentStatus.SCHEDULED)
                .modeOfConsultation(ConsultationMode.IN_PERSON)
                .build();
    }

    // ── bookAppointment ────────────────────────────────────────────────────────

    @Test
    void bookAppointment_noAuthHeader_returns200() throws Exception {
        BookAppointmentRequestDto req = BookAppointmentRequestDto.builder()
                .patientId(101L).providerId(1L).slotId(10L)
                .serviceType("General Consultation")
                .modeOfConsultation(ConsultationMode.IN_PERSON).build();

        when(appointmentService.bookAppointment(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(1L))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        verify(appointmentService).bookAppointment(any());
    }

    @Test
    void bookAppointment_withMatchingAuthUserId_returns200() throws Exception {
        BookAppointmentRequestDto req = BookAppointmentRequestDto.builder()
                .patientId(101L).providerId(1L).slotId(10L)
                .serviceType("General Consultation")
                .modeOfConsultation(ConsultationMode.IN_PERSON).build();

        when(appointmentService.bookAppointment(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/appointments")
                        .header("X-Authenticated-UserId", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void bookAppointment_withMismatchedAuthUserId_returns403() throws Exception {
        BookAppointmentRequestDto req = BookAppointmentRequestDto.builder()
                .patientId(101L).providerId(1L).slotId(10L)
                .serviceType("General Consultation")
                .modeOfConsultation(ConsultationMode.IN_PERSON).build();

        mockMvc.perform(post("/appointments")
                        .header("X-Authenticated-UserId", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_returns200() throws Exception {
        when(appointmentService.getById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(101L));
    }

    @Test
    void getByPatient_asPatient_matchingId_returns200() throws Exception {
        when(appointmentService.getByPatient(101L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/appointments/patient/101")
                        .header("X-Authenticated-UserId", "101")
                        .header("X-Authenticated-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByPatient_asAdmin_returns200() throws Exception {
        when(appointmentService.getByPatient(101L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/appointments/patient/101")
                        .header("X-Authenticated-UserId", "999")
                        .header("X-Authenticated-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getByPatient_asPatient_differentId_returns403() throws Exception {
        mockMvc.perform(get("/appointments/patient/101")
                        .header("X-Authenticated-UserId", "999")
                        .header("X-Authenticated-Role", "PATIENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByProvider_returns200() throws Exception {
        when(appointmentService.getByProvider(1L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/appointments/provider/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByProviderAndDate_returns200() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);
        when(appointmentService.getByProviderAndDate(1L, date)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/appointments/provider/1/date/" + date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void cancelAppointment_returns200() throws Exception {
        AppointmentResponseDto cancelled = AppointmentResponseDto.builder()
                .appointmentId(1L).status(AppointmentStatus.CANCELLED).build();
        when(appointmentService.cancelAppointment(1L)).thenReturn(cancelled);

        mockMvc.perform(put("/appointments/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void rescheduleAppointment_returns200() throws Exception {
        RescheduleAppointmentRequestDto req = RescheduleAppointmentRequestDto.builder()
                .newSlotId(20L).build();
        when(appointmentService.rescheduleAppointment(eq(1L), any())).thenReturn(sampleResponse);

        mockMvc.perform(put("/appointments/1/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void completeAppointment_returns200() throws Exception {
        AppointmentResponseDto completed = AppointmentResponseDto.builder()
                .appointmentId(1L).status(AppointmentStatus.COMPLETED).build();
        when(appointmentService.completeAppointment(1L)).thenReturn(completed);

        mockMvc.perform(put("/appointments/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void updateStatus_returns200() throws Exception {
        AppointmentResponseDto updated = AppointmentResponseDto.builder()
                .appointmentId(1L).status(AppointmentStatus.COMPLETED).build();
        when(appointmentService.updateStatus(1L, AppointmentStatus.COMPLETED)).thenReturn(updated);

        mockMvc.perform(put("/appointments/1/status")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getUpcomingByPatient_asPatient_returns200() throws Exception {
        when(appointmentService.getUpcomingByPatient(101L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/appointments/patient/101/upcoming")
                        .header("X-Authenticated-UserId", "101")
                        .header("X-Authenticated-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getUpcomingByPatient_asProvider_returns200() throws Exception {
        when(appointmentService.getUpcomingByPatient(101L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/appointments/patient/101/upcoming")
                        .header("X-Authenticated-UserId", "5")
                        .header("X-Authenticated-Role", "PROVIDER"))
                .andExpect(status().isOk());
    }

    @Test
    void getUpcomingByPatient_mismatchedId_returns403() throws Exception {
        mockMvc.perform(get("/appointments/patient/101/upcoming")
                        .header("X-Authenticated-UserId", "999")
                        .header("X-Authenticated-Role", "PATIENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAppointmentCount_returns200() throws Exception {
        when(appointmentService.getAppointmentCount(1L)).thenReturn(5L);

        mockMvc.perform(get("/appointments/provider/1/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));
    }

    @Test
    void getAllAppointments_asAdmin_returns200() throws Exception {
        when(appointmentService.getAllAppointments()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/appointments")
                        .header("X-Authenticated-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllAppointments_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/appointments")
                        .header("X-Authenticated-Role", "PATIENT"))
                .andExpect(status().isForbidden());
    }
}
