package com.chengmaomao.smartam.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegionCreateRequest {

    @NotBlank(message = "分区名称不能为空")
    private String name;

    @NotBlank(message = "分区标识不能为空")
    private String code;
}
