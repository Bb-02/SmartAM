package com.chengmaomao.smartam.tenant.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssetApplicationLog {
    private Long id;
    private Long applicationId;
    private Long assetId;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
