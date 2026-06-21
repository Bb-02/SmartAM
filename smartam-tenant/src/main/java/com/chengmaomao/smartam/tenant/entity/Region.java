package com.chengmaomao.smartam.tenant.entity;

import com.chengmaomao.smartam.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Region extends BaseEntity {
    private Long tenantId;
    private String name;
    private String code;
    private Integer isDefault;
    private Integer status;
}
