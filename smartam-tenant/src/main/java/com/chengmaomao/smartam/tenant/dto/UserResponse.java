package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private Long tenantId;
    private Long regionId;
    private Long deptId;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
