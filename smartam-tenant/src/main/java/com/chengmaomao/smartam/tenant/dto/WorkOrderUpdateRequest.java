package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

@Data
public class WorkOrderUpdateRequest {
    private String title;
    private String description;
    private String priority;
}
