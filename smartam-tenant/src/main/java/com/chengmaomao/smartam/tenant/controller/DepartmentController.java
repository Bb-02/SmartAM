package com.chengmaomao.smartam.tenant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chengmaomao.smartam.common.result.ApiResponse;
import com.chengmaomao.smartam.tenant.dto.DepartmentCreateRequest;
import com.chengmaomao.smartam.tenant.dto.DepartmentResponse;
import com.chengmaomao.smartam.tenant.dto.DepartmentTreeNode;
import com.chengmaomao.smartam.tenant.dto.DepartmentUpdateRequest;
import com.chengmaomao.smartam.tenant.service.DepartmentService;
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
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ApiResponse<DepartmentResponse> create(@Valid @RequestBody DepartmentCreateRequest req) {
        return ApiResponse.ok(departmentService.create(req));
    }

    @GetMapping("/tree")
    public ApiResponse<List<DepartmentTreeNode>> tree() {
        return ApiResponse.ok(departmentService.tree());
    }

    @GetMapping("/{id}")
    public ApiResponse<DepartmentResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(departmentService.getById(id));
    }

    @GetMapping
    public ApiResponse<IPage<DepartmentResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(departmentService.page(page, size, regionId, parentId, keyword));
    }

    @PutMapping("/{id}")
    public ApiResponse<DepartmentResponse> update(@PathVariable Long id,
                                                   @RequestBody DepartmentUpdateRequest req) {
        return ApiResponse.ok(departmentService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.ok();
    }
}
