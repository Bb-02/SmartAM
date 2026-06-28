package com.chengmaomao.smartam.tenant.controller;

import com.chengmaomao.smartam.common.result.ApiResponse;
import com.chengmaomao.smartam.tenant.dto.StatisticsOverviewResponse;
import com.chengmaomao.smartam.tenant.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    public ApiResponse<StatisticsOverviewResponse> overview() {
        return ApiResponse.ok(statisticsService.overview());
    }
}
