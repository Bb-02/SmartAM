package com.chengmaomao.smartam.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetCreateRequest {

    private Long deptId;
    private Long userId;

    @NotBlank(message = "资产名称不能为空")
    private String name;

    @NotBlank(message = "资产编号不能为空")
    private String code;

    @NotBlank(message = "资产品类不能为空")
    private String category;

    private String model;
    private String brand;
    private BigDecimal price;
    private Integer quantity;
    private String unit;
    private String location;
    private LocalDate purchaseDate;
    private LocalDate warrantyEnd;
    private String description;
}
