package com.pharmaflow.userhealth.saga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.userhealth.saga.controller.SagaController.UserRegistrationRequest;
import com.pharmaflow.userhealth.saga.model.SagaState;
import com.pharmaflow.userhealth.saga.service.UserRegistrationSagaOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SagaController.
 * Tests saga endpoints without requiring RabbitMQ.
 */
@WebMvcTest(SagaController.class)
@DisplayName("SagaController - Integration Tests")
class SagaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRegistrationSagaOrchestrator sagaOrchestrator;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/saga/register-user should initiate saga and return sagaId")
    void registerUser_shouldInitiateSagaAndReturnSagaId() throws Exception {
        // Given
        String sagaId = "test-saga-123";
        UserRegistrationRequest request = new UserRegistrationRequest(
                "John", "Doe", "john@test.com", "Password123!", "ROLE_USER"
        );

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(sagaOrchestrator.startUserRegistrationSaga(any())).thenReturn(sagaId);

        // When/Then
        mockMvc.perform(post("/api/saga/register-user")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("User registration saga initiated"))
                .andExpect(jsonPath("$.sagaId").value(sagaId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.note").value(containsString(sagaId)));

        verify(sagaOrchestrator, times(1)).startUserRegistrationSaga(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/saga/register-user with minimal data should use default role")
    void registerUser_withMinimalData_shouldUseDefaultRole() throws Exception {
        // Given
        String sagaId = "test-saga-456";
        UserRegistrationRequest request = new UserRegistrationRequest(
                "Jane", "Smith", "jane@test.com", "Password123!", null
        );

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(sagaOrchestrator.startUserRegistrationSaga(any())).thenReturn(sagaId);

        // When/Then
        mockMvc.perform(post("/api/saga/register-user")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sagaId").value(sagaId));

        verify(sagaOrchestrator, times(1)).startUserRegistrationSaga(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/saga/register-user should handle orchestrator failure")
    void registerUser_shouldHandleOrchestratorFailure() throws Exception {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest(
                "John", "Doe", "john@test.com", "Password123!", "ROLE_USER"
        );

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(sagaOrchestrator.startUserRegistrationSaga(any()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When/Then
        mockMvc.perform(post("/api/saga/register-user")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to initiate saga"))
                .andExpect(jsonPath("$.message").value(containsString("Database connection failed")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/saga/status/{sagaId} should return saga state when found")
    void getSagaStatus_shouldReturnSagaStateWhenFound() throws Exception {
        // Given
        String sagaId = "test-saga-123";
        SagaState sagaState = new SagaState();
        sagaState.setSagaId(sagaId);
        sagaState.setStatus(SagaState.SagaStatus.COMPLETED);
        sagaState.setCurrentStep("COMPLETED");
        sagaState.setEntityId(1L);
        sagaState.setCreatedAt(LocalDateTime.now());
        sagaState.setCompletedAt(LocalDateTime.now());

        when(sagaOrchestrator.getSagaState(sagaId)).thenReturn(Optional.of(sagaState));

        // When/Then
        mockMvc.perform(get("/api/saga/status/{sagaId}", sagaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sagaId").value(sagaId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currentStep").value("COMPLETED"))
                .andExpect(jsonPath("$.entityId").value(1))
                .andExpect(jsonPath("$.errorMessage").value(""))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.completedAt").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/saga/status/{sagaId} should return 404 when saga not found")
    void getSagaStatus_shouldReturn404WhenSagaNotFound() throws Exception {
        // Given
        String sagaId = "non-existent-saga";

        when(sagaOrchestrator.getSagaState(sagaId)).thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/api/saga/status/{sagaId}", sagaId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Saga not found"))
                .andExpect(jsonPath("$.sagaId").value(sagaId));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/saga/status/{sagaId} should return failed saga with error message")
    void getSagaStatus_shouldReturnFailedSagaWithErrorMessage() throws Exception {
        // Given
        String sagaId = "test-saga-failed";
        SagaState sagaState = new SagaState();
        sagaState.setSagaId(sagaId);
        sagaState.setStatus(SagaState.SagaStatus.FAILED);
        sagaState.setCurrentStep("INVENTORY_PENDING");
        sagaState.setEntityId(1L);
        sagaState.setErrorMessage("Inventory creation failed: Database timeout");
        sagaState.setCreatedAt(LocalDateTime.now());

        when(sagaOrchestrator.getSagaState(sagaId)).thenReturn(Optional.of(sagaState));

        // When/Then
        mockMvc.perform(get("/api/saga/status/{sagaId}", sagaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sagaId").value(sagaId))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value(containsString("Inventory creation failed")))
                .andExpect(jsonPath("$.completedAt").value(""));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/saga/status/{sagaId} should return compensated saga")
    void getSagaStatus_shouldReturnCompensatedSaga() throws Exception {
        // Given
        String sagaId = "test-saga-compensated";
        SagaState sagaState = new SagaState();
        sagaState.setSagaId(sagaId);
        sagaState.setStatus(SagaState.SagaStatus.COMPENSATED);
        sagaState.setCurrentStep("USER_DELETED");
        sagaState.setEntityId(1L);
        sagaState.setErrorMessage("Compensated due to inventory failure");
        sagaState.setCreatedAt(LocalDateTime.now());
        sagaState.setCompletedAt(LocalDateTime.now());

        when(sagaOrchestrator.getSagaState(sagaId)).thenReturn(Optional.of(sagaState));

        // When/Then
        mockMvc.perform(get("/api/saga/status/{sagaId}", sagaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sagaId").value(sagaId))
                .andExpect(jsonPath("$.status").value("COMPENSATED"))
                .andExpect(jsonPath("$.currentStep").value("USER_DELETED"))
                .andExpect(jsonPath("$.errorMessage").value(containsString("Compensated")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Controller should handle multiple saga registrations")
    void controller_shouldHandleMultipleSagaRegistrations() throws Exception {
        // Given
        UserRegistrationRequest request1 = new UserRegistrationRequest(
                "User1", "One", "user1@test.com", "Pass123!", "ROLE_USER"
        );
        UserRegistrationRequest request2 = new UserRegistrationRequest(
                "User2", "Two", "user2@test.com", "Pass123!", "ROLE_USER"
        );

        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(sagaOrchestrator.startUserRegistrationSaga(any()))
                .thenReturn("saga-1")
                .thenReturn("saga-2");

        // When/Then
        mockMvc.perform(post("/api/saga/register-user")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sagaId").value("saga-1"));

        mockMvc.perform(post("/api/saga/register-user")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sagaId").value("saga-2"));

        verify(sagaOrchestrator, times(2)).startUserRegistrationSaga(any());
    }
}


