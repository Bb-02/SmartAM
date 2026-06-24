package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

@Data
public class DepartmentUpdateRequest {
    private Long parentId;
    private String name;
    private String code;
}
