package com.pharmaflow.userhealth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.userhealth.dto.FamilyMemberCreateDTO;
import com.pharmaflow.userhealth.dto.FamilyMemberDTO;
import com.pharmaflow.userhealth.dto.PatientProfileDTO;
import com.pharmaflow.userhealth.exception.PatchOperationException;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.models.FamilyMember;
import com.pharmaflow.userhealth.models.PatientProfile;
import com.pharmaflow.userhealth.models.User;
import com.pharmaflow.userhealth.models.enums.Relationship;
import com.pharmaflow.userhealth.repositories.FamilyMemberRepository;
import com.pharmaflow.userhealth.repositories.UserRepository;
import com.pharmaflow.userhealth.specifications.FamilyMemberSpecs;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class FamilyMemberService {

    private static final Logger log = LoggerFactory.getLogger(FamilyMemberService.class);

    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public FamilyMemberService(FamilyMemberRepository familyMemberRepository,
                              UserRepository userRepository,
                              ModelMapper modelMapper,
                              ObjectMapper objectMapper) {
        this.familyMemberRepository = familyMemberRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<FamilyMemberDTO> findAll(Relationship relationship, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        Specification<FamilyMember> spec = Specification
            .where(FamilyMemberSpecs.hasRelationship(relationship));

        Page<FamilyMemberDTO> result = familyMemberRepository.findAll(spec, pageable)
            .map(this::convertToDTO);

        log.info("findAll executed in {} ms, returned {} of {} total family members",
                System.currentTimeMillis() - startTime,
                result.getNumberOfElements(),
                result.getTotalElements());

        return result;
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
        log.info("FamilyMember created with id: {}", savedMember.getId());
        return convertToDTO(savedMember);
    }

    @Transactional
    public List<FamilyMemberDTO> createFamilyMembersBatch(List<FamilyMemberCreateDTO> createDTOs) {
        long startTime = System.currentTimeMillis();
        List<FamilyMember> members = new ArrayList<>();

        for (FamilyMemberCreateDTO dto : createDTOs) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getUserId()));

            FamilyMember familyMember = new FamilyMember();
            familyMember.setFirstName(dto.getFirstName());
            familyMember.setRelationship(dto.getRelationship());
            familyMember.setUser(user);

            if (dto.getPatientProfile() != null) {
                PatientProfile profile = modelMapper.map(dto.getPatientProfile(), PatientProfile.class);
                familyMember.setPatientProfile(profile);
            }

            members.add(familyMember);
        }

        List<FamilyMember> savedMembers = familyMemberRepository.saveAll(members);
        log.info("Batch created {} family members in {} ms", savedMembers.size(), System.currentTimeMillis() - startTime);

        return savedMembers.stream()
                .map(this::convertToDTO)
                .toList();
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
    public FamilyMemberDTO patchFamilyMember(Long id, String patchDocument) {
        try {
            FamilyMember familyMember = familyMemberRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Family member not found with id: " + id));

            JsonNode memberJson = objectMapper.valueToTree(familyMember);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedMemberJson = patch.apply(memberJson);
            FamilyMember patchedMember = objectMapper.treeToValue(patchedMemberJson, FamilyMember.class);

            FamilyMember savedMember = familyMemberRepository.save(patchedMember);
            log.info("FamilyMember patched with id: {}", savedMember.getId());
            return convertToDTO(savedMember);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new PatchOperationException("Error applying patch: " + e.getMessage(), e);
        }
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

