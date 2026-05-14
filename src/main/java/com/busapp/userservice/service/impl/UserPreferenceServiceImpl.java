package com.busapp.userservice.service.impl;

import com.busapp.userservice.dto.request.UserPreferenceRequest;
import com.busapp.userservice.dto.response.UserPreferenceResponse;
import com.busapp.userservice.exception.ResourceNotFoundException;
import com.busapp.userservice.dto.mapper.UserPreferenceMapper;
import com.busapp.userservice.model.User;
import com.busapp.userservice.model.UserPreference;
import com.busapp.userservice.repository.UserPreferenceRepository;
import com.busapp.userservice.repository.UserRepository;
import com.busapp.userservice.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceRepository preferenceRepository;
    private final UserRepository           userRepository;
    private final UserPreferenceMapper     preferenceMapper;

    @Override
    public UserPreferenceResponse getByUserId(Long userId) {
        UserPreference preference = preferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Preferences not found for user: " + userId));
        return preferenceMapper.toResponse(preference);
    }

    @Override
    @Transactional
    public UserPreferenceResponse upsertPreference(Long userId, UserPreferenceRequest request) {
        if (preferenceRepository.existsByUserId(userId)) {
            UserPreference existing = preferenceRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Preferences not found for user: " + userId));
            existing.setTheme(request.getTheme());
            return preferenceMapper.toResponse(preferenceRepository.save(existing));
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        UserPreference preference = preferenceMapper.toEntity(request, user);
        return preferenceMapper.toResponse(preferenceRepository.save(preference));
    }
}
