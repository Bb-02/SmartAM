package com.chengmaomao.smartam.tenant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chengmaomao.smartam.common.result.ApiResponse;
import com.chengmaomao.smartam.tenant.dto.RegionCreateRequest;
import com.chengmaomao.smartam.tenant.dto.RegionResponse;
import com.chengmaomao.smartam.tenant.dto.RegionUpdateRequest;
import com.chengmaomao.smartam.tenant.service.RegionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @PostMapping
    public ApiResponse<RegionResponse> create(@Valid @RequestBody RegionCreateRequest req) {
        return ApiResponse.ok(regionService.create(req));
    }

    @GetMapping("/{id}")
    public ApiResponse<RegionResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(regionService.getById(id));
    }

    @GetMapping
    public ApiResponse<IPage<RegionResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(regionService.page(page, size, keyword));
    }

    @PutMapping("/{id}")
    public ApiResponse<RegionResponse> update(@PathVariable Long id,
                                               @RequestBody RegionUpdateRequest req) {
        return ApiResponse.ok(regionService.update(id, req));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                           @RequestParam Integer status) {
        regionService.updateStatus(id, status);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        regionService.delete(id);
        return ApiResponse.ok();
    }
}
