package com.chengmaomao.smartam.tenant.entity;

import com.chengmaomao.smartam.common.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    private Long tenantId;
    private Long regionId;
    private Long deptId;
    private String username;
    @JsonIgnore
    private String password;
    private String realName;
    private String phone;
    private String email;
    private String role;
    private Integer status;
}
