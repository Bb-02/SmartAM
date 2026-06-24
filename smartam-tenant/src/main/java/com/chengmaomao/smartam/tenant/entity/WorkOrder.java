package com.chengmaomao.smartam.tenant.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkOrder {
    private Long id;
    private Long tenantId;
    private Long regionId;
    private String type;
    private String title;
    private String description;
    private Long assetId;
    private Long reporterId;
    private Long engineerId;
    private String status;
    private String priority;
    private String resolution;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
