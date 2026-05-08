package com.gradhire.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String email;
    private String username;
    private String fullName;
    private String role;
    private boolean enabled;
    private String phone;
    private String department;
}
