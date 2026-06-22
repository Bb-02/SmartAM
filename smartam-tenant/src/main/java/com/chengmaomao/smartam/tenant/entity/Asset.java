package com.chengmaomao.smartam.tenant.entity;

import com.chengmaomao.smartam.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class Asset extends BaseEntity {
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
}
