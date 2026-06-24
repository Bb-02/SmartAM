package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkOrderResponse {
    private Long id;
    private Long tenantId;
    private Long regionId;
    private String type;
    private String title;
    private String description;
    private Long assetId;
    private Long reporterId;
    private String reporterName;
    private Long engineerId;
    private String engineerName;
    private String status;
    private String priority;
    private String resolution;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
