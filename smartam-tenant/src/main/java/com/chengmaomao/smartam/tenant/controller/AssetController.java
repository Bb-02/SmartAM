package com.chengmaomao.smartam.tenant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chengmaomao.smartam.common.result.ApiResponse;
import com.chengmaomao.smartam.tenant.dto.AssetCreateRequest;
import com.chengmaomao.smartam.tenant.dto.AssetResponse;
import com.chengmaomao.smartam.tenant.dto.AssetUpdateRequest;
import com.chengmaomao.smartam.tenant.entity.AssetLog;
import com.chengmaomao.smartam.tenant.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ApiResponse<AssetResponse> create(@Valid @RequestBody AssetCreateRequest req) {
        return ApiResponse.ok(assetService.create(req));
    }

    @GetMapping("/{id}")
    public ApiResponse<AssetResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(assetService.getById(id));
    }

    @GetMapping
    public ApiResponse<IPage<AssetResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String scope) {
        return ApiResponse.ok(assetService.page(page, size, status, category, regionId, deptId, userId, keyword, scope));
    }

    @PutMapping("/{id}")
    public ApiResponse<AssetResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody AssetUpdateRequest req) {
        return ApiResponse.ok(assetService.update(id, req));
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<AssetLog>> getLogs(@PathVariable Long id) {
        return ApiResponse.ok(assetService.getLogs(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ApiResponse.ok();
    }
}
