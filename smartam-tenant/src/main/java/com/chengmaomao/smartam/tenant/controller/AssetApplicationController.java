package com.chengmaomao.smartam.tenant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chengmaomao.smartam.common.result.ApiResponse;
import com.chengmaomao.smartam.tenant.dto.AssetApplicationCreateRequest;
import com.chengmaomao.smartam.tenant.dto.AssetApplicationResponse;
import com.chengmaomao.smartam.tenant.entity.AssetApplicationLog;
import com.chengmaomao.smartam.tenant.service.AssetApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/asset-applications")
@RequiredArgsConstructor
public class AssetApplicationController {

    private final AssetApplicationService applicationService;

    @PostMapping
    public ApiResponse<AssetApplicationResponse> create(@Valid @RequestBody AssetApplicationCreateRequest req) {
        return ApiResponse.ok(applicationService.create(req));
    }

    @GetMapping("/{id}")
    public ApiResponse<AssetApplicationResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.getById(id));
    }

    @GetMapping
    public ApiResponse<IPage<AssetApplicationResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) Long applicantId) {
        return ApiResponse.ok(applicationService.page(page, size, status, assetId, applicantId));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<AssetApplicationResponse> approve(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<AssetApplicationResponse> reject(@PathVariable Long id,
                                                         @RequestParam(required = false) String remark) {
        return ApiResponse.ok(applicationService.reject(id, remark));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<AssetApplicationResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.cancel(id));
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<AssetApplicationLog>> getLogs(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.getLogs(id));
    }
}
