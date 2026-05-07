package com.pharmaflow.pharmacyinventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.pharmacyinventory.dto.PharmacyCreateDTO;
import com.pharmaflow.pharmacyinventory.dto.PharmacyDTO;
import com.pharmaflow.pharmacyinventory.exception.PatchOperationException;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.service.PharmacyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    @MockBean
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
    void getPharmacies_ShouldReturn200AndPagedResult() throws Exception {
        Page<PharmacyDTO> page = new PageImpl<>(List.of(testPharmacyDTO));
        when(pharmacyService.findAll(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/pharmacies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Apoteka Centar"));

        verify(pharmacyService, times(1)).findAll(any(), any(), any());
    }

    @Test
    void getPharmacyById_WhenExists_ShouldReturn200() throws Exception {
        when(pharmacyService.getPharmacyById(1L)).thenReturn(testPharmacyDTO);

        mockMvc.perform(get("/api/pharmacies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Apoteka Centar"));

        verify(pharmacyService, times(1)).getPharmacyById(1L);
    }

    @Test
    void getPharmacyById_WhenNotExists_ShouldReturn404() throws Exception {
        when(pharmacyService.getPharmacyById(999L))
                .thenThrow(new ResourceNotFoundException("Pharmacy not found with id: 999"));

        mockMvc.perform(get("/api/pharmacies/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    void createPharmacy_WithValidData_ShouldReturn201() throws Exception {
        when(pharmacyService.createPharmacy(any(PharmacyCreateDTO.class))).thenReturn(testPharmacyDTO);

        mockMvc.perform(post("/api/pharmacies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

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
    void createPharmaciesBatch_ShouldReturn201() throws Exception {
        when(pharmacyService.createPharmaciesBatch(anyList())).thenReturn(List.of(testPharmacyDTO));

        mockMvc.perform(post("/api/pharmacies/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(testCreateDTO))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(pharmacyService, times(1)).createPharmaciesBatch(anyList());
    }

    @Test
    void updatePharmacy_WithValidData_ShouldReturn200() throws Exception {
        when(pharmacyService.updatePharmacy(eq(1L), any(PharmacyCreateDTO.class))).thenReturn(testPharmacyDTO);

        mockMvc.perform(put("/api/pharmacies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCreateDTO)))
                .andExpect(status().isOk());

        verify(pharmacyService, times(1)).updatePharmacy(eq(1L), any(PharmacyCreateDTO.class));
    }

    @Test
    void patchPharmacy_WithValidPatch_ShouldReturn200() throws Exception {
        String patch = "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"New Name\"}]";
        when(pharmacyService.patchPharmacy(eq(1L), anyString())).thenReturn(testPharmacyDTO);

        mockMvc.perform(patch("/api/pharmacies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isOk());

        verify(pharmacyService, times(1)).patchPharmacy(eq(1L), anyString());
    }

    @Test
    void patchPharmacy_WithInvalidPatch_ShouldReturn400() throws Exception {
        when(pharmacyService.patchPharmacy(eq(1L), anyString()))
                .thenThrow(new PatchOperationException("Bad patch"));

        mockMvc.perform(patch("/api/pharmacies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Patch Operation Failed"));
    }

    @Test
    void deletePharmacy_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(pharmacyService).deletePharmacy(1L);

        mockMvc.perform(delete("/api/pharmacies/1"))
                .andExpect(status().isNoContent());

        verify(pharmacyService, times(1)).deletePharmacy(1L);
    }

    @Test
    void deletePharmacy_WhenNotExists_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Pharmacy not found with id: 999"))
                .when(pharmacyService).deletePharmacy(999L);

        mockMvc.perform(delete("/api/pharmacies/999"))
                .andExpect(status().isNotFound());
    }
}
