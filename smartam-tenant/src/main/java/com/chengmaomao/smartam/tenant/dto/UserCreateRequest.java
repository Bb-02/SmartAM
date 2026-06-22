package com.chengmaomao.smartam.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotBlank(message = "角色不能为空")
    private String role;

    private Long regionId;
    private Long deptId;
    private String phone;
    private String email;
}
