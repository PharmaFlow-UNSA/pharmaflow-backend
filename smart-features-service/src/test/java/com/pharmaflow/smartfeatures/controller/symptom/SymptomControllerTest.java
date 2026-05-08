package com.pharmaflow.smartfeatures.controller.symptom;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaflow.smartfeatures.dto.symptom.SymptomRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomResponseDto;
import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.exception.GlobalExceptionHandler;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.service.symptom.SymptomService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SymptomController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class SymptomControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired @MockitoBean private SymptomService symptomService;

  private SymptomRequestDto validRequest;
  private SymptomResponseDto symptomResponse;

  @BeforeEach
  void setUp() {
    validRequest =
        new SymptomRequestDto(
            "Fever", "Elevated body temperature", SymptomSeverityLevel.HIGH, true);
    symptomResponse =
        new SymptomResponseDto(
            1L, "Fever", "Elevated body temperature", SymptomSeverityLevel.HIGH, true);
  }

  @Test
  void getAllSymptoms_ShouldReturn200AndActiveSymptoms() throws Exception {
    when(symptomService.getAllSymptoms()).thenReturn(List.of(symptomResponse));

    mockMvc
        .perform(get("/api/symptoms").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].name").value("Fever"))
        .andExpect(jsonPath("$[0].isActive").value(true));

    verify(symptomService).getAllSymptoms();
  }

  @Test
  void getSymptomById_WhenSymptomExists_ShouldReturn200() throws Exception {
    when(symptomService.getSymptomById(1L)).thenReturn(symptomResponse);

    mockMvc
        .perform(get("/api/symptoms/1").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Fever"))
        .andExpect(jsonPath("$.isActive").value(true));

    verify(symptomService).getSymptomById(1L);
  }

  @Test
  void getSymptomById_WhenSymptomMissing_ShouldReturn404() throws Exception {
    when(symptomService.getSymptomById(99L))
        .thenThrow(new ResourceNotFoundException("Symptom not found with id: 99"));

    mockMvc
        .perform(get("/api/symptoms/99").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Symptom not found with id: 99"));

    verify(symptomService).getSymptomById(99L);
  }

  @Test
  void getSymptomById_WhenSymptomIsInactive_ShouldReturn404() throws Exception {
    when(symptomService.getSymptomById(2L))
        .thenThrow(new ResourceNotFoundException("Symptom not found with id: 2"));

    mockMvc
        .perform(get("/api/symptoms/2").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

    verify(symptomService).getSymptomById(2L);
  }

  @Test
  void searchSymptoms_WithValidQuery_ShouldReturn200() throws Exception {
    when(symptomService.searchSymptoms("fev")).thenReturn(List.of(symptomResponse));

    mockMvc
        .perform(
            get("/api/symptoms/search")
                .param("query", "fev")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name").value("Fever"));

    verify(symptomService).searchSymptoms("fev");
  }

  @Test
  void searchSymptoms_WithTooShortQuery_ShouldReturn400() throws Exception {
    mockMvc
        .perform(
            get("/api/symptoms/search").param("query", "f").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("CONSTRAINT_VIOLATION"));

    verify(symptomService, never()).searchSymptoms(any());
  }

  @Test
  void createSymptom_WithValidPayload_ShouldReturn201() throws Exception {
    when(symptomService.createSymptom(any(SymptomRequestDto.class))).thenReturn(symptomResponse);

    mockMvc
        .perform(
            post("/api/symptoms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Fever"))
        .andExpect(jsonPath("$.isActive").value(true));

    verify(symptomService).createSymptom(any(SymptomRequestDto.class));
  }

  @Test
  void createSymptom_WithInvalidPayload_ShouldReturn400() throws Exception {
    mockMvc
        .perform(
            post("/api/symptoms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "name": "",
                                  "description": "desc"
                                }
                                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

    verify(symptomService, never()).createSymptom(any(SymptomRequestDto.class));
  }

  @Test
  void createSymptom_WithSanitizedTooShortName_ShouldReturn400() throws Exception {
    mockMvc
        .perform(
            post("/api/symptoms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "name": " a ",
                                  "description": "desc",
                                  "severityLevel": "LOW",
                                  "isActive": true
                                }
                                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.errors.name")
                .value("Symptom name must be between 2 and 100 characters after trimming"));

    verify(symptomService, never()).createSymptom(any(SymptomRequestDto.class));
  }

  @Test
  void createSymptom_WhenDuplicate_ShouldReturn409() throws Exception {
    when(symptomService.createSymptom(any(SymptomRequestDto.class)))
        .thenThrow(new DuplicateResourceException("Symptom with name 'Fever' already exists."));

    mockMvc
        .perform(
            post("/api/symptoms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
  }

  @Test
  void updateSymptom_WithValidPayload_ShouldReturn200() throws Exception {
    when(symptomService.updateSymptom(eq(1L), any(SymptomRequestDto.class)))
        .thenReturn(symptomResponse);

    mockMvc
        .perform(
            put("/api/symptoms/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Fever"));

    verify(symptomService).updateSymptom(eq(1L), any(SymptomRequestDto.class));
  }

  @Test
  void updateSymptom_WhenSymptomIsInactive_ShouldReturn404() throws Exception {
    when(symptomService.updateSymptom(eq(2L), any(SymptomRequestDto.class)))
        .thenThrow(new ResourceNotFoundException("Symptom not found with id: 2"));

    mockMvc
        .perform(
            put("/api/symptoms/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void deleteSymptom_WhenSymptomExists_ShouldReturn204() throws Exception {
    doNothing().when(symptomService).deleteSymptom(1L);

    mockMvc
        .perform(delete("/api/symptoms/1").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(symptomService).deleteSymptom(1L);
  }

  @Test
  void deleteSymptom_WhenSymptomIsInactive_ShouldReturn404() throws Exception {
    org.mockito.Mockito.doThrow(new ResourceNotFoundException("Symptom not found with id: 2"))
        .when(symptomService)
        .deleteSymptom(2L);

    mockMvc
        .perform(delete("/api/symptoms/2").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }

  private String validRequestJson() {
    return """
                {
                  "name": "Fever",
                  "description": "Elevated body temperature",
                  "severityLevel": "HIGH",
                  "isActive": true
                }
                """;
  }
}
