package com.pharmaflow.userhealth.controller;

import com.pharmaflow.userhealth.dto.FamilyMemberCreateDTO;
import com.pharmaflow.userhealth.dto.FamilyMemberDTO;
import com.pharmaflow.userhealth.service.FamilyMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family-members")
@Tag(name = "Family Member Management", description = "APIs for managing family members and their health profiles")
public class FamilyMemberController {

    private final FamilyMemberService familyMemberService;

    public FamilyMemberController(FamilyMemberService familyMemberService) {
        this.familyMemberService = familyMemberService;
    }

    @GetMapping
    @Operation(summary = "Get all family members", description = "Retrieves family members with optional pagination, sorting, and filtering. Use ?page=0&size=20&sort=lastName,asc for pagination. Use ?relationship=SPOUSE for filtering.")
    public ResponseEntity<List<FamilyMemberDTO>> getFamilyMembers(
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable,
            @RequestParam(required = false) String relationship,
            @RequestParam(required = false) Integer page) {

        // If filtering by relationship type
        if (relationship != null && !relationship.isEmpty()) {
            return ResponseEntity.ok(familyMemberService.findByRelationship(relationship));
        }

        // If pagination is explicitly requested
        if (page != null) {
            return ResponseEntity.ok(familyMemberService.getFamilyMembersPaginated(pageable).getContent());
        }

        // Default: return all family members
        return ResponseEntity.ok(familyMemberService.getAllFamilyMembers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get family member by ID", description = "Retrieves a specific family member by their ID")
    public ResponseEntity<FamilyMemberDTO> getFamilyMemberById(@PathVariable Long id) {
        return ResponseEntity.ok(familyMemberService.getFamilyMemberById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get family members by user ID", description = "Retrieves all family members of a specific user")
    public ResponseEntity<List<FamilyMemberDTO>> getFamilyMembersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(familyMemberService.getFamilyMembersByUserId(userId));
    }

    @PostMapping
    @Operation(summary = "Create a family member", description = "Creates a new family member with patient profile")
    public ResponseEntity<FamilyMemberDTO> createFamilyMember(@Valid @RequestBody FamilyMemberCreateDTO createDTO) {
        FamilyMemberDTO createdMember = familyMemberService.createFamilyMember(createDTO);
        return new ResponseEntity<>(createdMember, HttpStatus.CREATED);
    }

    @PostMapping(params = "bulk")
    @Operation(summary = "Create multiple family members (bulk)", description = "Bulk creation of family members. Use ?bulk=true")
    public ResponseEntity<List<FamilyMemberDTO>> createFamilyMembersBulk(
            @RequestBody @Valid List<@Valid FamilyMemberCreateDTO> createDTOs) {
        List<FamilyMemberDTO> createdMembers = familyMemberService.createFamilyMembersBatch(createDTOs);
        return new ResponseEntity<>(createdMembers, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update family member", description = "Updates an existing family member")
    public ResponseEntity<FamilyMemberDTO> updateFamilyMember(@PathVariable Long id,
                                                              @Valid @RequestBody FamilyMemberCreateDTO updateDTO) {
        return ResponseEntity.ok(familyMemberService.updateFamilyMember(id, updateDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update family member", description = "Applies JSON Patch operations to a family member")
    public ResponseEntity<FamilyMemberDTO> patchFamilyMember(@PathVariable Long id,
                                                             @RequestBody String patchDocument) {
        return ResponseEntity.ok(familyMemberService.patchFamilyMember(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete family member", description = "Deletes a family member by ID")
    public ResponseEntity<Void> deleteFamilyMember(@PathVariable Long id) {
        familyMemberService.deleteFamilyMember(id);
        return ResponseEntity.noContent().build();
    }
}

