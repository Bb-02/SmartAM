package com.chengmaomao.smartam.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chengmaomao.smartam.common.security.JwtUser;
import com.chengmaomao.smartam.tenant.dto.StatisticsOverviewResponse;
import com.chengmaomao.smartam.tenant.entity.*;
import com.chengmaomao.smartam.tenant.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final AssetMapper assetMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final RegionMapper regionMapper;
    private final WorkOrderMapper workOrderMapper;
    private final AssetApplicationMapper assetApplicationMapper;

    private JwtUser me() {
        return (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public StatisticsOverviewResponse overview() {
        JwtUser me = me();
        return switch (me.getRole()) {
            case RoleEnum.ADMIN_TENANT, RoleEnum.ADMIN_REGION, RoleEnum.ENGINEER -> buildFullStats(me);
            case RoleEnum.EMPLOYEE -> buildEmployeeStats(me);
            default -> new StatisticsOverviewResponse();
        };
    }

    private StatisticsOverviewResponse buildFullStats(JwtUser me) {
        StatisticsOverviewResponse r = new StatisticsOverviewResponse();
        r.setAssetTotal(assetCount(null));
        r.setAssetInStorage(assetCount(AssetStatus.IN_STORAGE));
        r.setAssetInUse(assetCount(AssetStatus.IN_USE));
        r.setAssetInRepair(assetCount(AssetStatus.IN_REPAIR));
        r.setAssetScrapped(assetCount(AssetStatus.SCRAPPED));
        r.setUserTotal(userCount());
        r.setDepartmentTotal(deptCount());
        r.setRegionTotal(regionCount());
        r.setWoPending(woCount(WorkOrderStatus.PENDING));
        r.setWoInWork(woCount(WorkOrderStatus.IN_WORK));
        r.setWoResolved(woCount(WorkOrderStatus.RESOLVED));
        r.setWoClosed(woCount(WorkOrderStatus.CLOSED));
        r.setWoCancelled(woCount(WorkOrderStatus.CANCELLED));
        r.setPendingApplications(pendingAppCount());
        return r;
    }

    private StatisticsOverviewResponse buildEmployeeStats(JwtUser me) {
        StatisticsOverviewResponse r = new StatisticsOverviewResponse();
        r.setAssetTotal(assetCount(null));
        r.setAssetInStorage(assetCount(AssetStatus.IN_STORAGE));
        r.setAssetInUse(assetCount(AssetStatus.IN_USE));
        r.setAssetInRepair(assetCount(AssetStatus.IN_REPAIR));
        r.setAssetScrapped(assetCount(AssetStatus.SCRAPPED));
        r.setWoPending(woCount(WorkOrderStatus.PENDING));
        r.setWoInWork(woCount(WorkOrderStatus.IN_WORK));
        r.setWoResolved(woCount(WorkOrderStatus.RESOLVED));
        r.setWoClosed(woCount(WorkOrderStatus.CLOSED));
        r.setWoCancelled(woCount(WorkOrderStatus.CANCELLED));
        return r;
    }

    private long assetCount(String status) {
        JwtUser me = me();
        LambdaQueryWrapper<Asset> qw = new LambdaQueryWrapper<>();
        qw.eq(Asset::getTenantId, me.getTenantId());
        if (RoleEnum.ADMIN_REGION.equals(me.getRole()) || RoleEnum.ENGINEER.equals(me.getRole())) {
            qw.eq(Asset::getRegionId, me.getRegionId());
        }
        if (RoleEnum.EMPLOYEE.equals(me.getRole())) {
            qw.eq(Asset::getDeptId, me.getDeptId());
        }
        if (status != null) {
            qw.eq(Asset::getStatus, status);
        }
        return assetMapper.selectCount(qw);
    }

    private long userCount() {
        JwtUser me = me();
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getTenantId, me.getTenantId());
        if (RoleEnum.ADMIN_REGION.equals(me.getRole())) {
            qw.eq(User::getRegionId, me.getRegionId());
        }
        return userMapper.selectCount(qw);
    }

    private long deptCount() {
        JwtUser me = me();
        LambdaQueryWrapper<Department> qw = new LambdaQueryWrapper<>();
        qw.eq(Department::getTenantId, me.getTenantId());
        if (RoleEnum.ADMIN_REGION.equals(me.getRole())) {
            qw.eq(Department::getRegionId, me.getRegionId());
        }
        return departmentMapper.selectCount(qw);
    }

    private long regionCount() {
        JwtUser me = me();
        if (RoleEnum.ADMIN_REGION.equals(me.getRole())) {
            return 1;
        }
        LambdaQueryWrapper<Region> qw = new LambdaQueryWrapper<>();
        qw.eq(Region::getTenantId, me.getTenantId());
        return regionMapper.selectCount(qw);
    }

    private long woCount(String status) {
        JwtUser me = me();
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(WorkOrder::getTenantId, me.getTenantId());
        if (RoleEnum.ADMIN_REGION.equals(me.getRole())) {
            qw.eq(WorkOrder::getRegionId, me.getRegionId());
        }
        if (RoleEnum.ENGINEER.equals(me.getRole())) {
            qw.eq(WorkOrder::getEngineerId, me.getUserId());
        }
        if (RoleEnum.EMPLOYEE.equals(me.getRole())) {
            qw.eq(WorkOrder::getReporterId, me.getUserId());
        }
        if (status != null) {
            qw.eq(WorkOrder::getStatus, status);
        }
        return workOrderMapper.selectCount(qw);
    }

    private long pendingAppCount() {
        JwtUser me = me();
        LambdaQueryWrapper<AssetApplication> qw = new LambdaQueryWrapper<>();
        qw.eq(AssetApplication::getTenantId, me.getTenantId());
        if (RoleEnum.ADMIN_REGION.equals(me.getRole())) {
            qw.eq(AssetApplication::getRegionId, me.getRegionId());
        }
        qw.eq(AssetApplication::getStatus, "PENDING");
        return assetApplicationMapper.selectCount(qw);
    }
}
