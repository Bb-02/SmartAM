package com.chengmaomao.smartam.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterTenantRequest {

    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    @NotBlank(message = "公司标识不能为空")
    private String companyCode;

    @NotBlank(message = "管理员账号不能为空")
    private String adminUsername;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;
}
