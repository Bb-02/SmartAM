package com.chengmaomao.smartam.tenant.entity;

import com.chengmaomao.smartam.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Department extends BaseEntity {
    private Long tenantId;
    private Long regionId;
    private Long parentId;
    private String name;
    private String code;
    private Integer status;
}
