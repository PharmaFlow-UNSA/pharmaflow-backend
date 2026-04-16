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
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FamilyMemberService {

    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public FamilyMemberService(FamilyMemberRepository familyMemberRepository,
                              UserRepository userRepository,
                              ModelMapper modelMapper) {
        this.familyMemberRepository = familyMemberRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public List<FamilyMemberDTO> getAllFamilyMembers() {
        return familyMemberRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public FamilyMemberDTO getFamilyMemberById(Long id) {
        FamilyMember familyMember = familyMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found with id: " + id));
        return convertToDTO(familyMember);
    }

    @Transactional(readOnly = true)
    public List<FamilyMemberDTO> getFamilyMembersByUserId(Long userId) {
        return familyMemberRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public FamilyMemberDTO createFamilyMember(FamilyMemberCreateDTO createDTO) {
        User user = userRepository.findById(createDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + createDTO.getUserId()));

        FamilyMember familyMember = new FamilyMember();
        familyMember.setFirstName(createDTO.getFirstName());
        familyMember.setRelationship(createDTO.getRelationship());
        familyMember.setUser(user);

        if (createDTO.getPatientProfile() != null) {
            PatientProfile profile = modelMapper.map(createDTO.getPatientProfile(), PatientProfile.class);
            familyMember.setPatientProfile(profile);
        }

        FamilyMember savedMember = familyMemberRepository.save(familyMember);
        return convertToDTO(savedMember);
    }

    @Transactional
    public FamilyMemberDTO updateFamilyMember(Long id, FamilyMemberCreateDTO updateDTO) {
        FamilyMember familyMember = familyMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found with id: " + id));

        familyMember.setFirstName(updateDTO.getFirstName());
        familyMember.setRelationship(updateDTO.getRelationship());

        if (updateDTO.getPatientProfile() != null) {
            if (familyMember.getPatientProfile() == null) {
                familyMember.setPatientProfile(new PatientProfile());
            }
            PatientProfile profile = familyMember.getPatientProfile();
            modelMapper.map(updateDTO.getPatientProfile(), profile);
        }

        FamilyMember updatedMember = familyMemberRepository.save(familyMember);
        return convertToDTO(updatedMember);
    }

    @Transactional
    public void deleteFamilyMember(Long id) {
        if (!familyMemberRepository.existsById(id)) {
            throw new ResourceNotFoundException("Family member not found with id: " + id);
        }
        familyMemberRepository.deleteById(id);
    }

    private FamilyMemberDTO convertToDTO(FamilyMember familyMember) {
        FamilyMemberDTO dto = new FamilyMemberDTO();
        dto.setId(familyMember.getId());
        dto.setFirstName(familyMember.getFirstName());
        dto.setRelationship(familyMember.getRelationship());
        dto.setUserId(familyMember.getUser().getId());
        
        if (familyMember.getPatientProfile() != null) {
            dto.setPatientProfile(modelMapper.map(familyMember.getPatientProfile(), PatientProfileDTO.class));
        }
        
        return dto;
    }
}

