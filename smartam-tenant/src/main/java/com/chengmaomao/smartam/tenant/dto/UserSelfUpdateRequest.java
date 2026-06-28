package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

@Data
public class UserSelfUpdateRequest {
    private String realName;
    private String phone;
    private String email;
}
