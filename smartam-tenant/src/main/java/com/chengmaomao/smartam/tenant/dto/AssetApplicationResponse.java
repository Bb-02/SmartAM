package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssetApplicationResponse {
    private Long id;
    private Long tenantId;
    private Long regionId;
    private Long assetId;
    private String assetName;
    private Long applicantId;
    private String applicantName;
    private String reason;
    private String status;
    private Long approverId;
    private String approverName;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
