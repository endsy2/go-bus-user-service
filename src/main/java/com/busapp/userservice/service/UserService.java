package com.busapp.userservice.service;

import com.busapp.userservice.dto.request.UpdateProfileRequest;
import com.busapp.userservice.dto.request.UserRequest;
import com.busapp.userservice.dto.response.UserBasicResponse;
import com.busapp.userservice.dto.response.UserResponseDetail;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

public interface UserService {

    List<UserResponseDetail> getAllUsers();

    /**
     * Get multiple users by their IDs in a single call (batch operation)
     */
    List<UserResponseDetail> getUsersByIds(Set<Long> userIds);

    UserResponseDetail getUserById(Long id);
    
    /**
     * Get basic user info by ID (lightweight - only essential fields)
     */
    UserBasicResponse getUserBasicById(Long id);

    List<UserBasicResponse>getUserBasicByIds(Set<Long> ids);

    UserResponseDetail getUserByEmail(String email);

    UserResponseDetail getUserByPhone(String phone);

    UserResponseDetail getUserByGoogleId(String googleId);

    UserResponseDetail createUser(UserRequest request);

    
    UserResponseDetail updateProfile(Long userId, UpdateProfileRequest request);

//    UserResponseDetail updateEmployee(Long id, UpdateEmployeeRequest request);

    void deleteUser(Long id);

    Page<UserResponseDetail> filterBySpec (int pageStart, int pageSize, Long userId, String email, String phone, String googleId, String userName, Boolean isEmployee,Boolean isDeleted,Boolean isActive);
    
    /**
     * Update user profile image reference
     */
    void updateProfileImage(Long userId, String imageObjectName);
    
    /**
     * Get user profile image object name
     */
    String getUserProfileImage(Long userId);
}
