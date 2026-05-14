package com.medibook.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medibook.payment.dto.*;
import com.medibook.payment.entity.PaymentMode;
import com.medibook.payment.entity.PaymentStatus;
import com.medibook.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock private PaymentService paymentService;
    @InjectMocks private PaymentController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private PaymentResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = PaymentResponseDto.builder()
                .paymentId(1L).appointmentId(10L).patientId(100L)
                .amount(500.0).status(PaymentStatus.SUCCESS)
                .mode(PaymentMode.UPI).transactionId("TXN_12345")
                .currency("INR").paidAt(LocalDateTime.now())
                .build();
    }

    @Test
    void processPayment_returns200() throws Exception {
        PaymentRequestDto req = PaymentRequestDto.builder()
                .appointmentId(10L).patientId(100L).amount(500.0).mode(PaymentMode.UPI).build();

        when(paymentService.processPayment(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1L))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentService).processPayment(any());
    }

    @Test
    void getPaymentById_returns200() throws Exception {
        when(paymentService.getPaymentById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN_12345"));
    }

    @Test
    void getPaymentByAppointmentId_returns200() throws Exception {
        when(paymentService.getPaymentByAppointmentId(10L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/payments/appointment/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(10L));
    }

    @Test
    void getPaymentsByPatient_returns200() throws Exception {
        when(paymentService.getPaymentsByPatient(100L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/payments/patient/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void refundPayment_returns200() throws Exception {
        PaymentResponseDto refunded = PaymentResponseDto.builder()
                .paymentId(1L).status(PaymentStatus.REFUNDED).build();
        when(paymentService.refundPayment(eq(1L), any())).thenReturn(refunded);

        RefundRequestDto req = RefundRequestDto.builder().reason("Patient cancelled").build();

        mockMvc.perform(put("/payments/1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void getAllPayments_returns200() throws Exception {
        when(paymentService.getAllPayments()).thenReturn(List.of(sampleResponse, sampleResponse));

        mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getPaymentsByPatient_emptyList_returns200() throws Exception {
        when(paymentService.getPaymentsByPatient(999L)).thenReturn(List.of());

        mockMvc.perform(get("/payments/patient/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
