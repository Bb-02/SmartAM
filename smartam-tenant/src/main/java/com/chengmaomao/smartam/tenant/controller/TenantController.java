package com.chengmaomao.smartam.tenant.controller;

import com.chengmaomao.smartam.common.result.ApiResponse;
import com.chengmaomao.smartam.tenant.dto.RegisterTenantRequest;
import com.chengmaomao.smartam.tenant.entity.Tenant;
import com.chengmaomao.smartam.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/register")
    public ApiResponse<Tenant> register(@Valid @RequestBody RegisterTenantRequest req) {
        Tenant tenant = tenantService.register(req);
        return ApiResponse.ok(tenant);
    }
}
