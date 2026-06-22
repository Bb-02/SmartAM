package com.chengmaomao.smartam.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    private String adminPassword;

    @NotBlank(message = "真实姓名不能为空")
    private String adminRealName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String adminPhone;

    @Email(message = "邮箱格式不正确")
    private String adminEmail;
}
