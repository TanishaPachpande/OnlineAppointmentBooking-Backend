package com.medibook.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medibook.notification.dto.*;
import com.medibook.notification.entity.NotificationStatus;
import com.medibook.notification.entity.NotificationType;
import com.medibook.notification.messaging.NotificationProducer;
import com.medibook.notification.service.NotificationService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationService notificationService;
    @Mock private NotificationProducer notificationProducer;
    @InjectMocks private NotificationController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private NotificationResponseDto sampleResponse;
    private NotificationRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = NotificationResponseDto.builder()
                .notificationId(1L).userId(100L).recipient("user@example.com")
                .type(NotificationType.EMAIL).subject("Test Subject").message("Test message")
                .status(NotificationStatus.SENT).createdAt(LocalDateTime.now())
                .build();

        sampleRequest = NotificationRequestDto.builder()
                .userId(100L).recipient("user@example.com")
                .type(NotificationType.EMAIL).subject("Test Subject").message("Test message")
                .build();
    }

    @Test
    void createNotification_returns200() throws Exception {
        when(notificationService.createAndSendNotification(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(1L))
                .andExpect(jsonPath("$.subject").value("Test Subject"));

        verify(notificationService).createAndSendNotification(any());
    }

    @Test
    void publishNotification_returns200() throws Exception {
        NotificationEventDto eventDto = NotificationEventDto.builder()
                .userId(100L).recipient("user@example.com")
                .type("EMAIL").subject("Test").message("Hello")
                .build();

        doNothing().when(notificationProducer).publishNotification(any());

        mockMvc.perform(post("/notifications/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification event published successfully"));

        verify(notificationProducer).publishNotification(any());
    }

    @Test
    void getNotificationById_returns200() throws Exception {
        when(notificationService.getNotificationById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(100L));
    }

    @Test
    void getNotificationsByUser_returns200() throws Exception {
        when(notificationService.getNotificationsByUser(100L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/notifications/user/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getNotificationsByUser_emptyList_returns200() throws Exception {
        when(notificationService.getNotificationsByUser(999L)).thenReturn(List.of());

        mockMvc.perform(get("/notifications/user/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllNotifications_returns200() throws Exception {
        when(notificationService.getAllNotifications()).thenReturn(List.of(sampleResponse, sampleResponse));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
