package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AssetResponse {
    private Long id;
    private Long tenantId;
    private Long regionId;
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
    private String regionName;
    private String deptName;
    private String userName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
