package com.pharmaflow.pharmacyinventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.pharmacyinventory.dto.PharmacyCreateDTO;
import com.pharmaflow.pharmacyinventory.dto.PharmacyDTO;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.service.PharmacyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PharmacyController.class)
@AutoConfigureMockMvc(addFilters = false)
class PharmacyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PharmacyService pharmacyService;

    private PharmacyDTO testPharmacyDTO;
    private PharmacyCreateDTO testCreateDTO;

    @BeforeEach
    void setUp() {
        testPharmacyDTO = new PharmacyDTO();
        testPharmacyDTO.setId(1L);
        testPharmacyDTO.setName("Apoteka Centar");
        testPharmacyDTO.setAddress("Marsala Tita 10");
        testPharmacyDTO.setCity("Sarajevo");
        testPharmacyDTO.setPhoneNumber("+387-33-123456");
        testPharmacyDTO.setEmail("centar@pharmaflow.ba");
        testPharmacyDTO.setOpeningHours("08:00-20:00");

        testCreateDTO = new PharmacyCreateDTO();
        testCreateDTO.setName("Apoteka Centar");
        testCreateDTO.setAddress("Marsala Tita 10");
        testCreateDTO.setCity("Sarajevo");
        testCreateDTO.setPhoneNumber("+387-33-123456");
        testCreateDTO.setEmail("centar@pharmaflow.ba");
        testCreateDTO.setOpeningHours("08:00-20:00");
    }

    @Test
    void getAllPharmacies_ShouldReturn200AndListOfPharmacies() throws Exception {
        when(pharmacyService.getAllPharmacies()).thenReturn(List.of(testPharmacyDTO));

        mockMvc.perform(get("/api/pharmacies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Apoteka Centar"));

        verify(pharmacyService, times(1)).getAllPharmacies();
    }

    @Test
    void getPharmacyById_WhenPharmacyExists_ShouldReturn200AndPharmacy() throws Exception {
        when(pharmacyService.getPharmacyById(1L)).thenReturn(testPharmacyDTO);

        mockMvc.perform(get("/api/pharmacies/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Apoteka Centar"))
                .andExpect(jsonPath("$.city").value("Sarajevo"));

        verify(pharmacyService, times(1)).getPharmacyById(1L);
    }

    @Test
    void getPharmacyById_WhenPharmacyNotExists_ShouldReturn404() throws Exception {
        when(pharmacyService.getPharmacyById(999L))
                .thenThrow(new ResourceNotFoundException("Pharmacy not found with id: 999"));

        mockMvc.perform(get("/api/pharmacies/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));

        verify(pharmacyService, times(1)).getPharmacyById(999L);
    }

    @Test
    void createPharmacy_WithValidData_ShouldReturn201AndCreatedPharmacy() throws Exception {
        when(pharmacyService.createPharmacy(any(PharmacyCreateDTO.class))).thenReturn(testPharmacyDTO);

        mockMvc.perform(post("/api/pharmacies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Apoteka Centar"));

        verify(pharmacyService, times(1)).createPharmacy(any(PharmacyCreateDTO.class));
    }

    @Test
    void createPharmacy_WithInvalidData_ShouldReturn400() throws Exception {
        PharmacyCreateDTO invalidDTO = new PharmacyCreateDTO();
        invalidDTO.setName("");
        invalidDTO.setAddress("Marsala Tita 10");
        invalidDTO.setCity("Sarajevo");

        mockMvc.perform(post("/api/pharmacies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));

        verify(pharmacyService, never()).createPharmacy(any(PharmacyCreateDTO.class));
    }

    @Test
    void updatePharmacy_WithValidData_ShouldReturn200AndUpdatedPharmacy() throws Exception {
        PharmacyDTO updatedDTO = new PharmacyDTO();
        updatedDTO.setId(1L);
        updatedDTO.setName("Apoteka Centar Updated");
        updatedDTO.setAddress("Marsala Tita 10");
        updatedDTO.setCity("Sarajevo");
        updatedDTO.setPhoneNumber("+387-33-123456");
        updatedDTO.setEmail("centar@pharmaflow.ba");
        updatedDTO.setOpeningHours("08:00-22:00");

        when(pharmacyService.updatePharmacy(eq(1L), any(PharmacyCreateDTO.class))).thenReturn(updatedDTO);

        PharmacyCreateDTO updateCreateDTO = new PharmacyCreateDTO();
        updateCreateDTO.setName("Apoteka Centar Updated");
        updateCreateDTO.setAddress("Marsala Tita 10");
        updateCreateDTO.setCity("Sarajevo");
        updateCreateDTO.setPhoneNumber("+387-33-123456");
        updateCreateDTO.setEmail("centar@pharmaflow.ba");
        updateCreateDTO.setOpeningHours("08:00-22:00");

        mockMvc.perform(put("/api/pharmacies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Apoteka Centar Updated"))
                .andExpect(jsonPath("$.openingHours").value("08:00-22:00"));

        verify(pharmacyService, times(1)).updatePharmacy(eq(1L), any(PharmacyCreateDTO.class));
    }

    @Test
    void deletePharmacy_WhenPharmacyExists_ShouldReturn204() throws Exception {
        doNothing().when(pharmacyService).deletePharmacy(1L);

        mockMvc.perform(delete("/api/pharmacies/1"))
                .andExpect(status().isNoContent());

        verify(pharmacyService, times(1)).deletePharmacy(1L);
    }

    @Test
    void deletePharmacy_WhenPharmacyNotExists_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Pharmacy not found with id: 999"))
                .when(pharmacyService).deletePharmacy(999L);

        mockMvc.perform(delete("/api/pharmacies/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));

        verify(pharmacyService, times(1)).deletePharmacy(999L);
    }
}
