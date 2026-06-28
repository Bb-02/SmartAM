package com.chengmaomao.smartam.tenant.service;

import com.chengmaomao.smartam.tenant.dto.DictResponse;
import com.chengmaomao.smartam.tenant.entity.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DictService {

    public DictResponse all() {
        DictResponse r = new DictResponse();
        r.setAssetStatuses(List.of(AssetStatus.IN_STORAGE, AssetStatus.IN_USE, AssetStatus.IN_REPAIR, AssetStatus.SCRAPPED));
        r.setWorkOrderStatuses(List.of(WorkOrderStatus.PENDING, WorkOrderStatus.IN_WORK, WorkOrderStatus.RESOLVED, WorkOrderStatus.CLOSED, WorkOrderStatus.CANCELLED));
        r.setWorkOrderTypes(List.of(WorkOrderType.REPAIR, WorkOrderType.CROSS_REGION));
        r.setWorkOrderPriorities(List.of("LOW", "NORMAL", "HIGH", "URGENT"));
        r.setApplicationStatuses(List.of(AssetApplicationStatus.PENDING, AssetApplicationStatus.APPROVED, AssetApplicationStatus.REJECTED, AssetApplicationStatus.CANCELLED));
        r.setApplicationTypes(List.of(AssetApplicationType.APPLY, AssetApplicationType.SCRAP, AssetApplicationType.TRANSFER));
        r.setRoles(List.of(RoleEnum.EMPLOYEE, RoleEnum.ENGINEER, RoleEnum.ADMIN_REGION, RoleEnum.ADMIN_TENANT));
        r.setMessageTypes(List.of("WORK_ORDER", "APPLICATION", "ASSET"));
        return r;
    }
}
