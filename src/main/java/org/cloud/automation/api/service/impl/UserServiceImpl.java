package org.cloud.automation.api.service.impl;

import lombok.RequiredArgsConstructor;
import org.cloud.automation.api.domain.User;
import org.cloud.automation.api.dto.UserDto;
import org.cloud.automation.api.repository.UserRepository;
import org.cloud.automation.api.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> findUserById(Long id) {
        return userRepository.findById(id).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        User user = toEntity(userDto);
        // Ensure ID is null so JPA creates a new entity
        user.setId(null);
        User savedUser = userRepository.save(user);
        return toDto(savedUser);
    }

    @Override
    @Transactional
    public Optional<UserDto> updateUser(Long id, UserDto userDto) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setName(userDto.getName());
                    existingUser.setEmail(userDto.getEmail());
                    existingUser.setRole(userDto.getRole());
                    User updatedUser = userRepository.save(existingUser);
                    return toDto(updatedUser);
                });
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        // This makes the delete operation idempotent.
        // If the resource doesn't exist, we don't need to do anything.
        userRepository.deleteById(id);
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private User toEntity(UserDto userDto) {
        return User.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .email(userDto.getEmail())
                .role(userDto.getRole())
                .build();
    }
}
