package com.pharmaflow.userhealth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.userhealth.dto.*;
import com.pharmaflow.userhealth.exception.DuplicateResourceException;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.models.PatientProfile;
import com.pharmaflow.userhealth.models.User;
import com.pharmaflow.userhealth.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public UserService(UserRepository userRepository, ModelMapper modelMapper,
                      PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        long startTime = System.currentTimeMillis();
        List<UserDTO> users = userRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
        log.info("getAllUsers executed in {} ms", System.currentTimeMillis() - startTime);
        return users;
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getUsersPaginated(Pageable pageable) {
        long startTime = System.currentTimeMillis();
        Page<UserDTO> users = userRepository.findAll(pageable)
                .map(this::convertToDTO);
        log.info("getUsersPaginated executed in {} ms, returned {} of {} total users",
                System.currentTimeMillis() - startTime, users.getNumberOfElements(), users.getTotalElements());
        return users;
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
        log.info("User created with id: {}", savedUser.getId());
        return convertToDTO(savedUser);
    }

    @Transactional
    public List<UserDTO> createUsersBatch(List<UserCreateDTO> userCreateDTOs) {
        long startTime = System.currentTimeMillis();
        List<User> users = new ArrayList<>();

        for (UserCreateDTO dto : userCreateDTOs) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new DuplicateResourceException("User with email " + dto.getEmail() + " already exists");
            }

            User user = new User();
            user.setFirstName(dto.getFirstName());
            user.setLastName(dto.getLastName());
            user.setEmail(dto.getEmail());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));

            if (dto.getPatientProfile() != null) {
                PatientProfile profile = modelMapper.map(dto.getPatientProfile(), PatientProfile.class);
                user.setPatientProfile(profile);
            }

            users.add(user);
        }

        List<User> savedUsers = userRepository.saveAll(users);
        log.info("Batch created {} users in {} ms", savedUsers.size(), System.currentTimeMillis() - startTime);

        return savedUsers.stream()
                .map(this::convertToDTO)
                .toList();
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
        log.info("User updated with id: {}", updatedUser.getId());
        return convertToDTO(updatedUser);
    }

    @Transactional
    public UserDTO patchUser(Long id, String patchDocument) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

            // Convert user to JSON
            JsonNode userJson = objectMapper.valueToTree(user);

            // Parse patch document
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));

            // Apply patch
            JsonNode patchedUserJson = patch.apply(userJson);

            // Convert back to User object
            User patchedUser = objectMapper.treeToValue(patchedUserJson, User.class);

            // Validate and save
            if (patchedUser.getEmail() != null && !patchedUser.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(patchedUser.getEmail())) {
                    throw new DuplicateResourceException("User with email " + patchedUser.getEmail() + " already exists");
                }
            }

            User savedUser = userRepository.save(patchedUser);
            log.info("User patched with id: {}", savedUser.getId());
            return convertToDTO(savedUser);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new RuntimeException("Error applying patch: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        log.info("User deleted with id: {}", id);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> findUsersByEmailDomain(String domain) {
        long startTime = System.currentTimeMillis();
        List<UserDTO> users = userRepository.findByEmailDomain(domain).stream()
                .map(this::convertToDTO)
                .toList();
        log.info("findUsersByEmailDomain executed in {} ms, found {} users",
                System.currentTimeMillis() - startTime, users.size());
        return users;
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

