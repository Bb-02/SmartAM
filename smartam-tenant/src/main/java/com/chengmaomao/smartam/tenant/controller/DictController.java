package com.chengmaomao.smartam.tenant.controller;

import com.chengmaomao.smartam.common.result.ApiResponse;
import com.chengmaomao.smartam.tenant.dto.DictResponse;
import com.chengmaomao.smartam.tenant.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping
    public ApiResponse<DictResponse> all() {
        return ApiResponse.ok(dictService.all());
    }
}
