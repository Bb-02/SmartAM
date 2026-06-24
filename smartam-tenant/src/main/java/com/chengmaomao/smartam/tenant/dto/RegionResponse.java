package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegionResponse {
    private Long id;
    private Long tenantId;
    private String name;
    private String code;
    private Integer isDefault;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
