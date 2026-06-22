package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetUpdateRequest {
    private Long deptId;
    private Long userId;
    private String name;
    private String code;
    private String category;
    private String model;
    private String brand;
    private BigDecimal price;
    private Integer quantity;
    private String unit;
    private String status;
    private String location;
    private LocalDate purchaseDate;
    private LocalDate warrantyEnd;
    private String description;
}
