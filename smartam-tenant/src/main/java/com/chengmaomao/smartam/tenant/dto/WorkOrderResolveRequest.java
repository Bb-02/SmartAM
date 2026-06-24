package com.chengmaomao.smartam.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkOrderResolveRequest {

    @NotBlank(message = "处理结果不能为空")
    private String resolution;
}
