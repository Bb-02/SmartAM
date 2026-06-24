package com.chengmaomao.smartam.tenant.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkOrderLog {
    private Long id;
    private Long workOrderId;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
