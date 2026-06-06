package com.exam.utility.service;

import com.exam.utility.dto.request.user.ChangeUserRoleRequest;
import com.exam.utility.dto.request.user.CreateUserRequest;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.auth.UserResponse;
import org.springframework.data.domain.Pageable;

/**
 * Manages internal staff user accounts (Operator, Finance, Manager, Admin).
 * Distinct from customer self-registration; all creation here is performed by an Administrator.
 */
public interface UserService {

    /** Creates a staff account, auto-generates a temporary password, and emails credentials. */
    UserResponse createUser(CreateUserRequest request);

    /** Returns a paginated list of all system users. */
    PagedResponse<UserResponse> getAllUsers(Pageable pageable);

    /** Returns a single user by their database ID. */
    UserResponse getUserById(Long id);

    /** Changes a user's role and sends an email notification of the change. */
    UserResponse changeUserRole(Long userId, ChangeUserRoleRequest request);

    /** Activates a previously deactivated user account. */
    void activateUser(Long userId);

    /** Deactivates a user account (soft delete — account is disabled, not removed). */
    void deactivateUser(Long userId);
}
