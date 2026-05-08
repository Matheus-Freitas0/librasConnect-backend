package com.librasConnect.system.dtos.response;

import java.time.Instant;
import java.util.Set;

import com.librasConnect.system.enums.Rule;
import com.librasConnect.system.models.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private Long id;
    private String name;
    private String email;
    private Set<Rule> rules;
    private Instant created;

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .rules(user.getRules())
                .created(user.getCreated())
                .build();
    }
}
