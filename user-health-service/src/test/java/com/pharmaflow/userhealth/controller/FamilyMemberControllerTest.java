package com.pharmaflow.userhealth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.userhealth.dto.FamilyMemberCreateDTO;
import com.pharmaflow.userhealth.dto.FamilyMemberDTO;
import com.pharmaflow.userhealth.dto.PatientProfileDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.service.FamilyMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FamilyMemberController.class)
class FamilyMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FamilyMemberService familyMemberService;

    private FamilyMemberDTO testMemberDTO;
    private FamilyMemberCreateDTO testCreateDTO;

    @BeforeEach
    void setUp() {
        testMemberDTO = new FamilyMemberDTO();
        testMemberDTO.setId(1L);
        testMemberDTO.setFirstName("John");
        testMemberDTO.setRelationship("Child");

        testCreateDTO = new FamilyMemberCreateDTO();
        testCreateDTO.setFirstName("John");
        testCreateDTO.setRelationship("Child");
        testCreateDTO.setUserId(1L);

        PatientProfileDTO profile = new PatientProfileDTO();
        profile.setWeight(50.0);
        profile.setHeight(150.0);
        profile.setBloodType("O+");
        testCreateDTO.setPatientProfile(profile);
    }

    @Test
    void getAllFamilyMembers_ShouldReturn200AndListOfMembers() throws Exception {
        List<FamilyMemberDTO> members = Arrays.asList(testMemberDTO);
        when(familyMemberService.getAllFamilyMembers()).thenReturn(members);

        mockMvc.perform(get("/api/family-members")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].relationship").value("Child"));

        verify(familyMemberService, times(1)).getAllFamilyMembers();
    }

    @Test
    void getFamilyMembers_WithRelationshipFilter_ShouldReturn200AndFilteredMembers() throws Exception {
        List<FamilyMemberDTO> members = Arrays.asList(testMemberDTO);
        when(familyMemberService.findByRelationship("Child")).thenReturn(members);

        mockMvc.perform(get("/api/family-members")
                        .param("relationship", "Child")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].relationship").value("Child"));

        verify(familyMemberService, times(1)).findByRelationship("Child");
        verify(familyMemberService, never()).getAllFamilyMembers();
    }

    @Test
    void getFamilyMemberById_WhenMemberExists_ShouldReturn200AndMember() throws Exception {
        when(familyMemberService.getFamilyMemberById(1L)).thenReturn(testMemberDTO);

        mockMvc.perform(get("/api/family-members/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(familyMemberService, times(1)).getFamilyMemberById(1L);
    }

    @Test
    void getFamilyMemberById_WhenMemberNotFound_ShouldReturn404() throws Exception {
        when(familyMemberService.getFamilyMemberById(999L))
                .thenThrow(new ResourceNotFoundException("Family member not found with id: 999"));

        mockMvc.perform(get("/api/family-members/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));

        verify(familyMemberService, times(1)).getFamilyMemberById(999L);
    }

    @Test
    void getFamilyMembersByUserId_ShouldReturn200AndListOfMembers() throws Exception {
        List<FamilyMemberDTO> members = Arrays.asList(testMemberDTO);
        when(familyMemberService.getFamilyMembersByUserId(1L)).thenReturn(members);

        mockMvc.perform(get("/api/family-members/user/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("John"));

        verify(familyMemberService, times(1)).getFamilyMembersByUserId(1L);
    }

    @Test
    void createFamilyMember_WithValidData_ShouldReturn201AndCreatedMember() throws Exception {
        when(familyMemberService.createFamilyMember(any(FamilyMemberCreateDTO.class)))
                .thenReturn(testMemberDTO);

        mockMvc.perform(post("/api/family-members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(familyMemberService, times(1)).createFamilyMember(any(FamilyMemberCreateDTO.class));
    }

    @Test
    void createFamilyMembers_WithBulkParameter_ShouldReturn201AndCreatedMembers() throws Exception {
        FamilyMemberCreateDTO member2DTO = new FamilyMemberCreateDTO();
        member2DTO.setFirstName("Sarah");
        member2DTO.setRelationship("Spouse");
        member2DTO.setUserId(1L);

        FamilyMemberDTO memberDTO2 = new FamilyMemberDTO();
        memberDTO2.setId(2L);
        memberDTO2.setFirstName("Sarah");
        memberDTO2.setRelationship("Spouse");

        List<FamilyMemberCreateDTO> members = Arrays.asList(testCreateDTO, member2DTO);
        when(familyMemberService.createFamilyMembersBatch(anyList()))
                .thenReturn(Arrays.asList(testMemberDTO, memberDTO2));

        mockMvc.perform(post("/api/family-members")
                        .param("bulk", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(members)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[1].firstName").value("Sarah"));

        verify(familyMemberService, times(1)).createFamilyMembersBatch(anyList());
    }

    @Test
    void createFamilyMember_WithInvalidData_ShouldReturn400() throws Exception {
        FamilyMemberCreateDTO invalidDTO = new FamilyMemberCreateDTO();
        invalidDTO.setFirstName("J"); // Too short
        invalidDTO.setRelationship("InvalidRelation"); // Not in enum
        invalidDTO.setUserId(null); // Required

        mockMvc.perform(post("/api/family-members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));

        verify(familyMemberService, never()).createFamilyMember(any(FamilyMemberCreateDTO.class));
        verify(familyMemberService, never()).createFamilyMembersBatch(anyList());
    }

    @Test
    void updateFamilyMember_WithValidData_ShouldReturn200AndUpdatedMember() throws Exception {
        when(familyMemberService.updateFamilyMember(eq(1L), any(FamilyMemberCreateDTO.class)))
                .thenReturn(testMemberDTO);

        mockMvc.perform(put("/api/family-members/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(familyMemberService, times(1)).updateFamilyMember(eq(1L), any(FamilyMemberCreateDTO.class));
    }

    @Test
    void deleteFamilyMember_WhenMemberExists_ShouldReturn204() throws Exception {
        doNothing().when(familyMemberService).deleteFamilyMember(1L);

        mockMvc.perform(delete("/api/family-members/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(familyMemberService, times(1)).deleteFamilyMember(1L);
    }

    @Test
    void deleteFamilyMember_WhenMemberNotFound_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Family member not found with id: 999"))
                .when(familyMemberService).deleteFamilyMember(999L);

        mockMvc.perform(delete("/api/family-members/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));

        verify(familyMemberService, times(1)).deleteFamilyMember(999L);
    }
}

