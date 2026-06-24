package com.chengmaomao.smartam.tenant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkOrderConfirmRequest {

    @NotNull(message = "请评分")
    @Min(1)
    @Max(5)
    private Integer rating;
}
