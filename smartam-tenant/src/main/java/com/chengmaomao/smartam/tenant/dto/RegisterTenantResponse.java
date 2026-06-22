package com.chengmaomao.smartam.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterTenantResponse {
    private Long tenantId;
    private String tenantName;
    private Long adminUserId;
}
