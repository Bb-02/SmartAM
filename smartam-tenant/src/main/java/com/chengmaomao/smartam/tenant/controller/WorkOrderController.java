package com.chengmaomao.smartam.tenant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chengmaomao.smartam.common.result.ApiResponse;
import com.chengmaomao.smartam.tenant.dto.WorkOrderConfirmRequest;
import com.chengmaomao.smartam.tenant.dto.WorkOrderCreateRequest;
import com.chengmaomao.smartam.tenant.dto.WorkOrderResolveRequest;
import com.chengmaomao.smartam.tenant.dto.WorkOrderResponse;
import com.chengmaomao.smartam.tenant.entity.WorkOrderLog;
import com.chengmaomao.smartam.tenant.service.WorkOrderService;
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
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping
    public ApiResponse<WorkOrderResponse> create(@Valid @RequestBody WorkOrderCreateRequest req) {
        return ApiResponse.ok(workOrderService.create(req));
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkOrderResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(workOrderService.getById(id));
    }

    @GetMapping
    public ApiResponse<IPage<WorkOrderResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        return ApiResponse.ok(workOrderService.page(page, size, status, priority));
    }

    @PostMapping("/{id}/claim")
    public ApiResponse<WorkOrderResponse> claim(@PathVariable Long id) {
        return ApiResponse.ok(workOrderService.claim(id));
    }

    @PostMapping("/{id}/resolve")
    public ApiResponse<WorkOrderResponse> resolve(@PathVariable Long id,
                                                   @Valid @RequestBody WorkOrderResolveRequest req) {
        return ApiResponse.ok(workOrderService.resolve(id, req));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<WorkOrderResponse> confirm(@PathVariable Long id,
                                                   @Valid @RequestBody WorkOrderConfirmRequest req) {
        return ApiResponse.ok(workOrderService.confirm(id, req));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<WorkOrderResponse> reject(@PathVariable Long id,
                                                  @RequestParam(required = false) String remark) {
        return ApiResponse.ok(workOrderService.reject(id, remark));
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<WorkOrderLog>> getLogs(@PathVariable Long id) {
        return ApiResponse.ok(workOrderService.getLogs(id));
    }
}
