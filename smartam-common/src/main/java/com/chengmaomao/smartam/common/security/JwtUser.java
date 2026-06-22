package com.chengmaomao.smartam.common.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtUser {
    private Long userId;
    private Long tenantId;
    private Long regionId;
    private Long deptId;
    private String username;
    private String role;
}
