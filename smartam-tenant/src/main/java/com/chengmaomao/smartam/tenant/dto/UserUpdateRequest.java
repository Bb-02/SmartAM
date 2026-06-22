package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String realName;
    private String password;
    private String phone;
    private String email;
    private Long regionId;
    private Long deptId;
    private Integer status;
}
