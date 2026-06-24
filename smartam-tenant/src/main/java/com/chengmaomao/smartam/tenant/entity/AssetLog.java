package com.chengmaomao.smartam.tenant.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssetLog {
    private Long id;
    private Long tenantId;
    private Long assetId;
    private Long operatorId;
    private String action;
    private String description;
    private String remark;
    private LocalDateTime createdAt;
}
