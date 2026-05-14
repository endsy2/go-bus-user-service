package com.busapp.userservice.service.impl;

import com.busapp.userservice.dto.admin.PermissionRequest;
import com.busapp.userservice.dto.admin.PermissionResponse;
import com.busapp.userservice.exception.DuplicateResourceException;
import com.busapp.userservice.exception.ResourceNotFoundException;
import com.busapp.userservice.dto.mapper.PermissionMapper;
import com.busapp.userservice.model.Permission;
import com.busapp.userservice.repository.PermissionRepository;
import com.busapp.userservice.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper     permissionMapper;

    @Override
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(permissionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PermissionResponse getPermissionById(Long id) {
        return permissionMapper.toResponse(findPermission(id));
    }

    @Override
    @Transactional
    public PermissionResponse createPermission(PermissionRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Permission already exists: " + request.getName());
        }
        return permissionMapper.toResponse(permissionRepository.save(permissionMapper.toEntity(request)));
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(Long id, PermissionRequest request) {
        Permission perm = findPermission(id);
        perm.setName(request.getName());
        perm.setDescription(request.getDescription());
        return permissionMapper.toResponse(permissionRepository.save(perm));
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        if (!permissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Permission not found: " + id);
        }
        permissionRepository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Permission findPermission(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + id));
    }
}
