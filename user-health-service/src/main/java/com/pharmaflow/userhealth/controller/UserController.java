package com.pharmaflow.userhealth.controller;

import com.pharmaflow.userhealth.dto.UserCreateDTO;
import com.pharmaflow.userhealth.dto.UserDTO;
import com.pharmaflow.userhealth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users and their health profiles")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves users with optional pagination, sorting, and filtering. Use ?page=0&size=10&sort=id,asc for pagination. Use ?emailDomain=example.com for filtering.")
    public ResponseEntity<List<UserDTO>> getUsers(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String emailDomain,
            @RequestParam(required = false) Integer page) {

        // If filtering by email domain
        if (emailDomain != null && !emailDomain.isEmpty()) {
            return ResponseEntity.ok(userService.findUsersByEmailDomain(emailDomain));
        }

        // If pagination is explicitly requested (page parameter present)
        if (page != null) {
            return ResponseEntity.ok(userService.getUsersPaginated(pageable).getContent());
        }

        // Default: return all users
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a specific user by their ID")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @Operation(summary = "Create a user", description = "Creates a new user with patient profile")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserCreateDTO userCreateDTO) {
        UserDTO createdUser = userService.createUser(userCreateDTO);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PostMapping(params = "bulk")
    @Operation(summary = "Create multiple users (bulk)", description = "Bulk creation of users. Use ?bulk=true")
    public ResponseEntity<List<UserDTO>> createUsersBulk(
            @RequestBody @Valid List<@Valid UserCreateDTO> userCreateDTOs) {
        List<UserDTO> createdUsers = userService.createUsersBatch(userCreateDTOs);
        return new ResponseEntity<>(createdUsers, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates an existing user")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, 
                                             @Valid @RequestBody UserCreateDTO userCreateDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userCreateDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update user", description = "Applies JSON Patch operations to a user")
    public ResponseEntity<UserDTO> patchUser(@PathVariable Long id,
                                            @RequestBody String patchDocument) {
        return ResponseEntity.ok(userService.patchUser(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user by ID")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

