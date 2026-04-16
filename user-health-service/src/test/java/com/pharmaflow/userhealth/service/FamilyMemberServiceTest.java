package com.pharmaflow.userhealth.service;

import com.pharmaflow.userhealth.dto.FamilyMemberCreateDTO;
import com.pharmaflow.userhealth.dto.FamilyMemberDTO;
import com.pharmaflow.userhealth.dto.PatientProfileDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.models.FamilyMember;
import com.pharmaflow.userhealth.models.PatientProfile;
import com.pharmaflow.userhealth.models.User;
import com.pharmaflow.userhealth.repositories.FamilyMemberRepository;
import com.pharmaflow.userhealth.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamilyMemberServiceTest {

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private FamilyMemberService familyMemberService;

    private FamilyMember testMember;
    private FamilyMemberDTO testMemberDTO;
    private FamilyMemberCreateDTO testCreateDTO;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@pharmaflow.ba");

        PatientProfile profile = new PatientProfile();
        profile.setId(1L);
        profile.setWeight(50.0);
        profile.setHeight(150.0);
        profile.setBloodType("O+");

        testMember = new FamilyMember();
        testMember.setId(1L);
        testMember.setFirstName("John");
        testMember.setRelationship("Child");
        testMember.setUser(testUser);
        testMember.setPatientProfile(profile);

        testMemberDTO = new FamilyMemberDTO();
        testMemberDTO.setId(1L);
        testMemberDTO.setFirstName("John");
        testMemberDTO.setRelationship("Child");

        testCreateDTO = new FamilyMemberCreateDTO();
        testCreateDTO.setFirstName("John");
        testCreateDTO.setRelationship("Child");
        testCreateDTO.setUserId(1L);

        PatientProfileDTO profileDTO = new PatientProfileDTO();
        profileDTO.setWeight(50.0);
        profileDTO.setHeight(150.0);
        profileDTO.setBloodType("O+");
        testCreateDTO.setPatientProfile(profileDTO);
    }

    @Test
    void getAllFamilyMembers_ShouldReturnListOfMembers() {
        when(familyMemberRepository.findAll()).thenReturn(Arrays.asList(testMember));
        when(modelMapper.map(any(PatientProfile.class), eq(PatientProfileDTO.class)))
                .thenReturn(new PatientProfileDTO());

        List<FamilyMemberDTO> result = familyMemberService.getAllFamilyMembers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
        verify(familyMemberRepository, times(1)).findAll();
    }

    @Test
    void getFamilyMemberById_WhenMemberExists_ShouldReturnMember() {
        when(familyMemberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(modelMapper.map(any(PatientProfile.class), eq(PatientProfileDTO.class)))
                .thenReturn(new PatientProfileDTO());

        FamilyMemberDTO result = familyMemberService.getFamilyMemberById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John", result.getFirstName());
        verify(familyMemberRepository, times(1)).findById(1L);
    }

    @Test
    void getFamilyMemberById_WhenMemberNotFound_ShouldThrowException() {
        when(familyMemberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> familyMemberService.getFamilyMemberById(999L));
        verify(familyMemberRepository, times(1)).findById(999L);
    }

    @Test
    void getFamilyMembersByUserId_ShouldReturnListOfMembers() {
        when(familyMemberRepository.findByUserId(1L)).thenReturn(Arrays.asList(testMember));
        when(modelMapper.map(any(PatientProfile.class), eq(PatientProfileDTO.class)))
                .thenReturn(new PatientProfileDTO());

        List<FamilyMemberDTO> result = familyMemberService.getFamilyMembersByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(familyMemberRepository, times(1)).findByUserId(1L);
    }

    @Test
    void createFamilyMember_WhenUserExists_ShouldReturnCreatedMember() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(PatientProfileDTO.class), eq(PatientProfile.class))).thenReturn(new PatientProfile());
        when(modelMapper.map(any(PatientProfile.class), eq(PatientProfileDTO.class)))
                .thenReturn(new PatientProfileDTO());
        when(familyMemberRepository.save(any(FamilyMember.class))).thenReturn(testMember);

        FamilyMemberDTO result = familyMemberService.createFamilyMember(testCreateDTO);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(userRepository, times(1)).findById(1L);
        verify(familyMemberRepository, times(1)).save(any(FamilyMember.class));
    }

    @Test
    void createFamilyMember_WhenUserNotFound_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        testCreateDTO.setUserId(999L);

        assertThrows(ResourceNotFoundException.class, () -> familyMemberService.createFamilyMember(testCreateDTO));
        verify(userRepository, times(1)).findById(999L);
        verify(familyMemberRepository, never()).save(any(FamilyMember.class));
    }

    @Test
    void updateFamilyMember_WhenMemberExists_ShouldReturnUpdatedMember() {
        when(familyMemberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(familyMemberRepository.save(any(FamilyMember.class))).thenReturn(testMember);
        when(modelMapper.map(any(PatientProfile.class), eq(PatientProfileDTO.class)))
                .thenReturn(new PatientProfileDTO());

        // ModelMapper.map(DTO, entity) doesn't return anything - it modifies in place
        doNothing().when(modelMapper).map(any(PatientProfileDTO.class), any(PatientProfile.class));

        FamilyMemberDTO result = familyMemberService.updateFamilyMember(1L, testCreateDTO);

        assertNotNull(result);
        verify(familyMemberRepository, times(1)).findById(1L);
        verify(familyMemberRepository, times(1)).save(any(FamilyMember.class));
    }

    @Test
    void updateFamilyMember_WhenMemberNotFound_ShouldThrowException() {
        when(familyMemberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> familyMemberService.updateFamilyMember(999L, testCreateDTO));
        verify(familyMemberRepository, times(1)).findById(999L);
        verify(familyMemberRepository, never()).save(any(FamilyMember.class));
    }

    @Test
    void deleteFamilyMember_WhenMemberExists_ShouldDeleteMember() {
        when(familyMemberRepository.existsById(1L)).thenReturn(true);
        doNothing().when(familyMemberRepository).deleteById(1L);

        familyMemberService.deleteFamilyMember(1L);

        verify(familyMemberRepository, times(1)).existsById(1L);
        verify(familyMemberRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteFamilyMember_WhenMemberNotFound_ShouldThrowException() {
        when(familyMemberRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> familyMemberService.deleteFamilyMember(999L));
        verify(familyMemberRepository, times(1)).existsById(999L);
        verify(familyMemberRepository, never()).deleteById(any());
    }
}

