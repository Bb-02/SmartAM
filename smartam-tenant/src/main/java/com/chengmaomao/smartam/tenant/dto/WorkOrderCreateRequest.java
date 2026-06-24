package com.chengmaomao.smartam.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkOrderCreateRequest {

    private String type;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;
    private Long assetId;
    private String priority;
}
