package com.pharmaflow.userhealth.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.userhealth.dto.UserCreateDTO;
import com.pharmaflow.userhealth.dto.UserDTO;
import com.pharmaflow.userhealth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Arrays;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private UserService userService;
    private UserDTO testUserDTO;
    private UserCreateDTO testUserCreateDTO;
    @BeforeEach
    void setUp() {
        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setFirstName("John");
        testUserDTO.setLastName("Doe");
        testUserDTO.setEmail("test@test.com");
        testUserCreateDTO = new UserCreateDTO();
        testUserCreateDTO.setFirstName("John");
        testUserCreateDTO.setLastName("Doe");
        testUserCreateDTO.setEmail("test@test.com");
        testUserCreateDTO.setPassword("Password123!");
    }
    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers() throws Exception {
        Page<UserDTO> page = new PageImpl<>(Arrays.asList(testUserDTO));
        when(userService.findAll(any(), any())).thenReturn(page);
        mockMvc.perform(get("/api/users")).andExpect(status().isOk());
        verify(userService, times(1)).findAll(any(), any());
    }
    @Test
    @WithMockUser(roles = "USER")
    void getUserById() throws Exception {
        when(userService.getUserById(1L)).thenReturn(testUserDTO);
        mockMvc.perform(get("/api/users/1")).andExpect(status().isOk());
        verify(userService, times(1)).getUserById(1L);
    }
    @Test
    @WithMockUser(roles = "USER")
    void createUser() throws Exception {
        when(userService.createUser(any())).thenReturn(testUserDTO);
        mockMvc.perform(post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserCreateDTO)))
                .andExpect(status().isCreated());
        verify(userService, times(1)).createUser(any());
    }
}