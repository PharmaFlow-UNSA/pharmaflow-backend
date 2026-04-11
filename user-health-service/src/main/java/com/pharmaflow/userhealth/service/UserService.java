package com.pharmaflow.userhealth.service;

import com.pharmaflow.userhealth.dto.*;
import com.pharmaflow.userhealth.exception.DuplicateResourceException;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.models.PatientProfile;
import com.pharmaflow.userhealth.models.User;
import com.pharmaflow.userhealth.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }

    @Transactional
    public UserDTO createUser(UserCreateDTO userCreateDTO) {
        if (userRepository.existsByEmail(userCreateDTO.getEmail())) {
            throw new DuplicateResourceException("User with email " + userCreateDTO.getEmail() + " already exists");
        }

        User user = new User();
        user.setFirstName(userCreateDTO.getFirstName());
        user.setLastName(userCreateDTO.getLastName());
        user.setEmail(userCreateDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userCreateDTO.getPassword()));

        if (userCreateDTO.getPatientProfile() != null) {
            PatientProfile profile = modelMapper.map(userCreateDTO.getPatientProfile(), PatientProfile.class);
            user.setPatientProfile(profile);
        }

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    @Transactional
    public UserDTO updateUser(Long id, UserCreateDTO userCreateDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check if email is being changed to an existing email
        if (!user.getEmail().equals(userCreateDTO.getEmail()) && 
            userRepository.existsByEmail(userCreateDTO.getEmail())) {
            throw new DuplicateResourceException("User with email " + userCreateDTO.getEmail() + " already exists");
        }

        user.setFirstName(userCreateDTO.getFirstName());
        user.setLastName(userCreateDTO.getLastName());
        user.setEmail(userCreateDTO.getEmail());
        
        if (userCreateDTO.getPassword() != null && !userCreateDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userCreateDTO.getPassword()));
        }

        if (userCreateDTO.getPatientProfile() != null) {
            if (user.getPatientProfile() == null) {
                user.setPatientProfile(new PatientProfile());
            }
            PatientProfile profile = user.getPatientProfile();
            modelMapper.map(userCreateDTO.getPatientProfile(), profile);
        }

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        // Don't expose password
        
        if (user.getPatientProfile() != null) {
            dto.setPatientProfile(modelMapper.map(user.getPatientProfile(), PatientProfileDTO.class));
        }
        
        if (user.getFamilyMembers() != null) {
            dto.setFamilyMemberIds(user.getFamilyMembers().stream()
                    .map(fm -> fm.getId())
                    .toList());
        }
        
        return dto;
    }
}

