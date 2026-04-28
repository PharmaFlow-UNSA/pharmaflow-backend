package com.pharmaflow.userhealth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.userhealth.dto.UserCreateDTO;
import com.pharmaflow.userhealth.dto.UserDTO;
import com.pharmaflow.userhealth.exception.ResourceNotFoundException;
import com.pharmaflow.userhealth.service.UserService;
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

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserDTO testUserDTO;
    private UserCreateDTO testUserCreateDTO;

    @BeforeEach
    void setUp() {
        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setFirstName("John");
        testUserDTO.setLastName("Doe");
        testUserDTO.setEmail("john.doe@pharmaflow.ba");

        testUserCreateDTO = new UserCreateDTO();
        testUserCreateDTO.setFirstName("John");
        testUserCreateDTO.setLastName("Doe");
        testUserCreateDTO.setEmail("john.doe@pharmaflow.ba");
        testUserCreateDTO.setPassword("Password123!");
    }

    @Test
    void getAllUsers_ShouldReturn200AndListOfUsers() throws Exception {
        // Arrange
        List<UserDTO> users = Arrays.asList(testUserDTO);
        when(userService.getAllUsers()).thenReturn(users);

        // Act & Assert
        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].email").value("john.doe@pharmaflow.ba"));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void getUsers_WithEmailDomainFilter_ShouldReturn200AndFilteredUsers() throws Exception {
        // Arrange
        List<UserDTO> users = Arrays.asList(testUserDTO);
        when(userService.findUsersByEmailDomain("pharmaflow.ba")).thenReturn(users);

        // Act & Assert
        mockMvc.perform(get("/api/users")
                        .param("emailDomain", "pharmaflow.ba")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("john.doe@pharmaflow.ba"));

        verify(userService, times(1)).findUsersByEmailDomain("pharmaflow.ba");
        verify(userService, never()).getAllUsers();
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturn200AndUser() throws Exception {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(testUserDTO);

        // Act & Assert
        mockMvc.perform(get("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("john.doe@pharmaflow.ba"));

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    void getUserById_WhenUserNotExists_ShouldReturn404() throws Exception {
        // Arrange
        when(userService.getUserById(999L)).thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with id: 999"));

        verify(userService, times(1)).getUserById(999L);
    }

    @Test
    void createUser_WithValidData_ShouldReturn201AndCreatedUser() throws Exception {
        // Arrange
        when(userService.createUser(any(UserCreateDTO.class))).thenReturn(testUserDTO);

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUserCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(userService, times(1)).createUser(any(UserCreateDTO.class));
    }

    @Test
    void createUsers_WithBulkParameter_ShouldReturn201AndCreatedUsers() throws Exception {
        // Arrange
        UserCreateDTO user2 = new UserCreateDTO();
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setEmail("jane.smith@pharmaflow.ba");
        user2.setPassword("Password123!");

        List<UserCreateDTO> users = Arrays.asList(testUserCreateDTO, user2);

        UserDTO userDTO2 = new UserDTO();
        userDTO2.setId(2L);
        userDTO2.setFirstName("Jane");
        userDTO2.setLastName("Smith");
        userDTO2.setEmail("jane.smith@pharmaflow.ba");

        when(userService.createUsersBatch(anyList())).thenReturn(Arrays.asList(testUserDTO, userDTO2));

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .param("bulk", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(users)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[1].firstName").value("Jane"));

        verify(userService, times(1)).createUsersBatch(anyList());
    }

    @Test
    void createUser_WithInvalidData_ShouldReturn400() throws Exception {
        // Arrange - Invalid email and short first name
        UserCreateDTO invalidDTO = new UserCreateDTO();
        invalidDTO.setFirstName("J"); // Too short
        invalidDTO.setLastName("Doe");
        invalidDTO.setEmail("invalid-email"); // Invalid format
        invalidDTO.setPassword("pass"); // Too short

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));

        verify(userService, never()).createUser(any(UserCreateDTO.class));
        verify(userService, never()).createUsersBatch(anyList());
    }

    @Test
    void updateUser_WithValidData_ShouldReturn200AndUpdatedUser() throws Exception {
        // Arrange
        when(userService.updateUser(eq(1L), any(UserCreateDTO.class))).thenReturn(testUserDTO);

        // Act & Assert
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUserCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(userService, times(1)).updateUser(eq(1L), any(UserCreateDTO.class));
    }

    @Test
    void deleteUser_WhenUserExists_ShouldReturn204() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    void deleteUser_WhenUserNotExists_ShouldReturn404() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("User not found with id: 999"))
                .when(userService).deleteUser(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));

        verify(userService, times(1)).deleteUser(999L);
    }
}

