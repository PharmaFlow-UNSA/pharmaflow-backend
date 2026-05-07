package com.pharmaflow.userhealth.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.userhealth.dto.FamilyMemberDTO;
import com.pharmaflow.userhealth.models.enums.Relationship;
import com.pharmaflow.userhealth.service.FamilyMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(FamilyMemberController.class)
class FamilyMemberControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private FamilyMemberService familyMemberService;
    private FamilyMemberDTO testMemberDTO;
    @BeforeEach
    void setUp() {
        testMemberDTO = new FamilyMemberDTO();
        testMemberDTO.setId(1L);
        testMemberDTO.setFirstName("John");
        testMemberDTO.setRelationship(Relationship.CHILD);
    }
    @Test
    void getMemberById() throws Exception {
        when(familyMemberService.getFamilyMemberById(1L)).thenReturn(testMemberDTO);
        mockMvc.perform(get("/api/family-members/1")).andExpect(status().isOk());
        verify(familyMemberService, times(1)).getFamilyMemberById(1L);
    }
}