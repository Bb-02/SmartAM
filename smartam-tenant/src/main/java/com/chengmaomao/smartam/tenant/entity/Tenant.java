package com.chengmaomao.smartam.tenant.entity;

import com.chengmaomao.smartam.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Tenant extends BaseEntity {
    private String name;
    private String code;
    private Integer status;
}
