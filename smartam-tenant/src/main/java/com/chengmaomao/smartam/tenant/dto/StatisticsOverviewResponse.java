package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

@Data
public class StatisticsOverviewResponse {
    private long assetTotal;
    private long assetInStorage;
    private long assetInUse;
    private long assetInRepair;
    private long assetScrapped;
    private long userTotal;
    private long departmentTotal;
    private long regionTotal;
    private long woPending;
    private long woInWork;
    private long woResolved;
    private long woClosed;
    private long woCancelled;
    private long pendingApplications;
}
