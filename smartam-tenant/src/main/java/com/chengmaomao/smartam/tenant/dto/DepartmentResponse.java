package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DepartmentResponse {
    private Long id;
    private Long tenantId;
    private Long regionId;
    private Long parentId;
    private String name;
    private String code;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
