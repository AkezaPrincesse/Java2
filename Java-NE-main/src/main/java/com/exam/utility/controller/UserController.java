package com.exam.utility.controller;

import com.exam.utility.dto.request.user.ChangeUserRoleRequest;
import com.exam.utility.dto.request.user.CreateUserRequest;
import com.exam.utility.dto.response.ApiResponse;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.auth.UserResponse;
import com.exam.utility.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Administrator-only endpoints for managing internal staff user accounts.
 *
 * All endpoints require ADMIN role. User creation auto-generates a temporary password
 * and emails credentials to the new user. The user must change the password on first login.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "Admin-only: create and manage internal staff accounts")
public class UserController {

    private final UserService userService;

    /**
     * Creates a new staff user account. A temporary password is generated and emailed.
     * The account is ACTIVE immediately with forcePasswordChange = true.
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Create a new staff user account (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("User account created. Credentials sent to " + request.getEmail(), response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "List all system users (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved",
            userService.getAllUsers(PageRequest.of(page, size, sort))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get a user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved", userService.getUserById(id)));
    }

    /**
     * Changes a user's role. An email notification is automatically sent to the user
     * describing the new role and its permissions. Change is audit-logged.
     */
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Change a user's role (triggers email notification)")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
        @PathVariable Long id,
        @Valid @RequestBody ChangeUserRoleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", userService.changeUserRole(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Activate a user account")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User account activated"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Deactivate a user account (soft disable)")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User account deactivated"));
    }
}
