package com.chengmaomao.smartam.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chengmaomao.smartam.common.exception.BusinessException;
import com.chengmaomao.smartam.common.security.JwtUser;
import com.chengmaomao.smartam.tenant.dto.AssetCreateRequest;
import com.chengmaomao.smartam.tenant.dto.AssetResponse;
import com.chengmaomao.smartam.tenant.dto.AssetUpdateRequest;
import com.chengmaomao.smartam.tenant.entity.Asset;
import com.chengmaomao.smartam.tenant.entity.AssetLog;
import com.chengmaomao.smartam.tenant.entity.AssetStatus;
import com.chengmaomao.smartam.tenant.entity.Department;
import com.chengmaomao.smartam.tenant.entity.Region;
import com.chengmaomao.smartam.tenant.entity.RoleEnum;
import com.chengmaomao.smartam.tenant.entity.User;
import com.chengmaomao.smartam.tenant.entity.WorkOrder;
import com.chengmaomao.smartam.tenant.entity.WorkOrderLog;
import com.chengmaomao.smartam.tenant.entity.WorkOrderStatus;
import com.chengmaomao.smartam.tenant.mapper.DepartmentMapper;
import com.chengmaomao.smartam.tenant.mapper.UserMapper;
import com.chengmaomao.smartam.tenant.mapper.AssetLogMapper;
import com.chengmaomao.smartam.tenant.mapper.AssetMapper;
import com.chengmaomao.smartam.tenant.mapper.DepartmentMapper;
import com.chengmaomao.smartam.tenant.mapper.RegionMapper;
import com.chengmaomao.smartam.tenant.mapper.UserMapper;
import com.chengmaomao.smartam.tenant.mapper.WorkOrderLogMapper;
import com.chengmaomao.smartam.tenant.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetMapper assetMapper;
    private final AssetLogMapper assetLogMapper;
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderLogMapper workOrderLogMapper;
    private final RegionMapper regionMapper;
    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;

    private JwtUser currentUser() {
        return (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** 按角色注入数据范围过滤 */
    private void applyRoleFilter(LambdaQueryWrapper<Asset> qw, JwtUser user) {
        qw.eq(Asset::getTenantId, user.getTenantId());
        switch (user.getRole()) {
            case RoleEnum.EMPLOYEE:
                qw.eq(Asset::getRegionId, user.getRegionId())
                  .eq(Asset::getDeptId, user.getDeptId());
                break;
            case RoleEnum.ENGINEER:
            case RoleEnum.ADMIN_REGION:
                qw.eq(Asset::getRegionId, user.getRegionId());
                break;
            // ADMIN_TENANT: only tenant_id
        }
    }

    /** 检查当前用户是否有权操作该资产 */
    private Asset getOwnedAsset(Long assetId) {
        JwtUser user = currentUser();
        Asset asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }
        if (!asset.getTenantId().equals(user.getTenantId())) {
            throw new BusinessException("资产不存在");
        }
        if (RoleEnum.ADMIN_TENANT.equals(user.getRole())) {
            return asset;
        }
        if (!asset.getRegionId().equals(user.getRegionId())) {
            throw new BusinessException("无权操作该资产");
        }
        return asset;
    }

    @Transactional
    public AssetResponse create(AssetCreateRequest req) {
        JwtUser user = currentUser();
        if (!RoleEnum.ADMIN_REGION.equals(user.getRole()) && !RoleEnum.ADMIN_TENANT.equals(user.getRole())) {
            throw new BusinessException("无权限创建资产");
        }

        if (req.getCode() != null) {
            Long codeCount = assetMapper.selectCount(new LambdaQueryWrapper<Asset>()
                    .eq(Asset::getTenantId, user.getTenantId())
                    .eq(Asset::getCode, req.getCode()));
            if (codeCount > 0) {
                throw new BusinessException("该资产编码已存在");
            }
        }

        Asset asset = new Asset();
        asset.setTenantId(user.getTenantId());
        // ADMIN_TENANT 可指定分区，否则用自身分区
        if (RoleEnum.ADMIN_TENANT.equals(user.getRole()) && req.getRegionId() != null) {
            Region region = regionMapper.selectById(req.getRegionId());
            if (region == null || !region.getTenantId().equals(user.getTenantId())) {
                throw new BusinessException("分区不存在");
            }
            asset.setRegionId(req.getRegionId());
        } else {
            asset.setRegionId(user.getRegionId());
        }
        asset.setDeptId(req.getDeptId());
        asset.setUserId(req.getUserId());
        validateAssignment(asset.getRegionId(), asset.getDeptId(), asset.getUserId(), user.getTenantId());
        asset.setName(req.getName());
        asset.setCode(req.getCode());
        asset.setCategory(req.getCategory());
        asset.setModel(req.getModel());
        asset.setBrand(req.getBrand());
        asset.setPrice(req.getPrice());
        asset.setQuantity(req.getQuantity() != null ? req.getQuantity() : 1);
        asset.setUnit(req.getUnit());
        asset.setStatus(AssetStatus.IN_STORAGE);
        asset.setLocation(req.getLocation());
        asset.setPurchaseDate(req.getPurchaseDate());
        asset.setWarrantyEnd(req.getWarrantyEnd());
        asset.setDescription(req.getDescription());
        assetMapper.insert(asset);
        asset = assetMapper.selectById(asset.getId());

        writeLog(asset, user.getUserId(), "CREATE",
                asset.getName() + "（" + asset.getCode() + "）入库，品类 " + asset.getCategory(), null);

        return toResponse(asset);
    }

    public AssetResponse getById(Long id) {
        Asset asset = getOwnedAsset(id);
        JwtUser user = currentUser();
        if (RoleEnum.EMPLOYEE.equals(user.getRole())
                && !asset.getDeptId().equals(user.getDeptId())) {
            throw new BusinessException("无权查看该资产");
        }
        return toResponse(asset);
    }

    public IPage<AssetResponse> page(int page, int size, String status, String category,
                                     Long regionId, Long deptId, String keyword) {
        JwtUser user = currentUser();
        LambdaQueryWrapper<Asset> qw = new LambdaQueryWrapper<>();
        applyRoleFilter(qw, user);

        // ADMIN_TENANT 可额外按分区筛选
        if (regionId != null && RoleEnum.ADMIN_TENANT.equals(user.getRole())) {
            qw.eq(Asset::getRegionId, regionId);
        }
        if (StringUtils.hasText(status)) {
            qw.eq(Asset::getStatus, status);
        }
        if (StringUtils.hasText(category)) {
            qw.eq(Asset::getCategory, category);
        }
        if (deptId != null) {
            qw.eq(Asset::getDeptId, deptId);
        }
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Asset::getName, keyword).or().like(Asset::getCode, keyword));
        }
        qw.orderByDesc(Asset::getId);

        Page<Asset> result = assetMapper.selectPage(Page.of(page, size), qw);
        return result.convert(this::toResponse);
    }

    @Transactional
    public AssetResponse update(Long id, AssetUpdateRequest req) {
        Asset old = getOwnedAsset(id);
        JwtUser user = currentUser();
        if (RoleEnum.EMPLOYEE.equals(user.getRole()) || RoleEnum.ENGINEER.equals(user.getRole())) {
            throw new BusinessException("无权限编辑资产");
        }

        // 保存旧值用于变更日志
        String oldStatus = old.getStatus();
        Long oldRegionId = old.getRegionId();
        Long oldDeptId = old.getDeptId();
        Long oldUserId = old.getUserId();

        if (req.getRegionId() != null) {
            if (RoleEnum.ADMIN_TENANT.equals(user.getRole())) {
                Region region = regionMapper.selectById(req.getRegionId());
                if (region == null || !region.getTenantId().equals(user.getTenantId())) {
                    throw new BusinessException("分区不存在");
                }
                old.setRegionId(req.getRegionId());
            } else {
                throw new BusinessException("仅租户管理员可变更资产归属分区");
            }
        }
        if (req.getDeptId() != null) old.setDeptId(req.getDeptId());
        if (req.getUserId() != null) old.setUserId(req.getUserId());
        if (req.getName() != null) old.setName(req.getName());
        if (req.getCode() != null) {
            Long codeCount = assetMapper.selectCount(new LambdaQueryWrapper<Asset>()
                    .eq(Asset::getTenantId, user.getTenantId())
                    .eq(Asset::getCode, req.getCode())
                    .ne(Asset::getId, id));
            if (codeCount > 0) {
                throw new BusinessException("该资产编码已存在");
            }
            old.setCode(req.getCode());
        }
        if (req.getCategory() != null) old.setCategory(req.getCategory());
        if (req.getModel() != null) old.setModel(req.getModel());
        if (req.getBrand() != null) old.setBrand(req.getBrand());
        if (req.getPrice() != null) old.setPrice(req.getPrice());
        if (req.getQuantity() != null) old.setQuantity(req.getQuantity());
        if (req.getUnit() != null) old.setUnit(req.getUnit());
        if (req.getStatus() != null) {
            if (!AssetStatus.isValid(req.getStatus())) {
                throw new BusinessException("无效的资产状态: " + req.getStatus());
            }
            if (AssetStatus.SCRAPPED.equals(oldStatus) && !AssetStatus.SCRAPPED.equals(req.getStatus())) {
                throw new BusinessException("已报废的资产不可恢复");
            }
            old.setStatus(req.getStatus());
        }
        if (req.getLocation() != null) old.setLocation(req.getLocation());
        if (req.getPurchaseDate() != null) old.setPurchaseDate(req.getPurchaseDate());
        if (req.getWarrantyEnd() != null) old.setWarrantyEnd(req.getWarrantyEnd());
        if (req.getDescription() != null) old.setDescription(req.getDescription());

        // 校验部门、用户、分区一致性
        if (req.getDeptId() != null || req.getUserId() != null || req.getRegionId() != null) {
            validateAssignment(old.getRegionId(), old.getDeptId(), old.getUserId(), old.getTenantId());
        }

        // 未显式设置状态时，分配领用人自动从 IN_STORAGE 切为 IN_USE
        if (req.getStatus() == null
                && oldUserId == null && old.getUserId() != null
                && AssetStatus.IN_STORAGE.equals(oldStatus)) {
            old.setStatus(AssetStatus.IN_USE);
        }

        // 构建变更描述
        List<String> changes = new ArrayList<>();
        if (!Objects.equals(oldStatus, old.getStatus())) {
            changes.add("状态: " + oldStatus + " → " + old.getStatus());
        }
        if (!Objects.equals(oldDeptId, old.getDeptId())) {
            changes.add("部门: " + desc(oldDeptId) + " → " + desc(old.getDeptId()));
        }
        if (!Objects.equals(oldUserId, old.getUserId())) {
            changes.add("领用人: " + desc(oldUserId) + " → " + desc(old.getUserId()));
        }
        if (!Objects.equals(oldRegionId, old.getRegionId())) {
            changes.add("分区: " + desc(oldRegionId) + " → " + desc(old.getRegionId()));
        }

        assetMapper.updateById(old);

        // 资产报废时，取消所有关联的活跃工单
        if (AssetStatus.SCRAPPED.equals(old.getStatus()) && !AssetStatus.SCRAPPED.equals(oldStatus)) {
            List<WorkOrder> activeOrders = workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                    .eq(WorkOrder::getAssetId, id)
                    .notIn(WorkOrder::getStatus, WorkOrderStatus.CLOSED, WorkOrderStatus.CANCELLED));
            for (WorkOrder wo : activeOrders) {
                String prevStatus = wo.getStatus();
                wo.setStatus(WorkOrderStatus.CANCELLED);
                workOrderMapper.updateById(wo);
                WorkOrderLog woLog = new WorkOrderLog();
                woLog.setWorkOrderId(wo.getId());
                woLog.setFromStatus(prevStatus);
                woLog.setToStatus(WorkOrderStatus.CANCELLED);
                woLog.setOperatorId(user.getUserId());
                woLog.setRemark("资产已报废，工单自动取消");
                workOrderLogMapper.insert(woLog);
            }
        }

        if (!changes.isEmpty()) {
            String action = inferAction(oldStatus, old.getStatus());
            writeLog(old, user.getUserId(), action, String.join("; ", changes), null);
        }

        return toResponse(old);
    }

    private String inferAction(String fromStatus, String toStatus) {
        if (AssetStatus.IN_STORAGE.equals(fromStatus) && AssetStatus.IN_USE.equals(toStatus)) return "ASSIGN";
        if (AssetStatus.IN_USE.equals(fromStatus) && AssetStatus.IN_STORAGE.equals(toStatus)) return "RETURN";
        if (AssetStatus.IN_USE.equals(fromStatus) && AssetStatus.IN_REPAIR.equals(toStatus)) return "REPAIR";
        if (AssetStatus.IN_REPAIR.equals(fromStatus) && AssetStatus.IN_USE.equals(toStatus)) return "REPAIR_DONE";
        if (AssetStatus.SCRAPPED.equals(toStatus)) return "SCRAP";
        return "UPDATE";
    }

    private static String desc(Object val) {
        return val == null ? "空" : val.toString();
    }

    private void validateAssignment(Long regionId, Long deptId, Long userId, Long tenantId) {
        if (deptId != null) {
            Department dept = departmentMapper.selectById(deptId);
            if (dept == null || !dept.getTenantId().equals(tenantId)) {
                throw new BusinessException("部门不存在");
            }
            if (!dept.getRegionId().equals(regionId)) {
                throw new BusinessException("部门不属于该分区");
            }
        }
        if (userId != null) {
            User u = userMapper.selectById(userId);
            if (u == null || !u.getTenantId().equals(tenantId)) {
                throw new BusinessException("用户不存在");
            }
            if (deptId != null && !deptId.equals(u.getDeptId())) {
                throw new BusinessException("用户不属于该部门");
            }
        }
    }

    private void writeLog(Asset asset, Long operatorId, String action, String description, String remark) {
        AssetLog log = new AssetLog();
        log.setTenantId(asset.getTenantId());
        log.setAssetId(asset.getId());
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setDescription(description);
        log.setRemark(remark);
        assetLogMapper.insert(log);
    }

    public List<AssetLog> getLogs(Long assetId) {
        Asset asset = getOwnedAsset(assetId);
        JwtUser user = currentUser();
        if (RoleEnum.EMPLOYEE.equals(user.getRole())
                && !asset.getDeptId().equals(user.getDeptId())) {
            throw new BusinessException("无权查看该资产");
        }
        return assetLogMapper.selectList(new LambdaQueryWrapper<AssetLog>()
                .eq(AssetLog::getAssetId, assetId)
                .orderByDesc(AssetLog::getCreatedAt));
    }

    @Transactional
    public void delete(Long id) {
        Asset asset = getOwnedAsset(id);
        JwtUser user = currentUser();
        if (!RoleEnum.ADMIN_TENANT.equals(user.getRole())) {
            throw new BusinessException("仅租户管理员可删除资产");
        }

        Long woCount = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getAssetId, id)
                .notIn(WorkOrder::getStatus, WorkOrderStatus.CLOSED, WorkOrderStatus.CANCELLED));
        if (woCount > 0) {
            throw new BusinessException("存在关联此资产的活跃工单，无法删除");
        }

        assetMapper.deleteById(asset.getId());
    }

    private AssetResponse toResponse(Asset a) {
        AssetResponse r = new AssetResponse();
        r.setId(a.getId());
        r.setTenantId(a.getTenantId());
        r.setRegionId(a.getRegionId());
        r.setDeptId(a.getDeptId());
        r.setUserId(a.getUserId());
        r.setName(a.getName());
        r.setCode(a.getCode());
        r.setCategory(a.getCategory());
        r.setModel(a.getModel());
        r.setBrand(a.getBrand());
        r.setPrice(a.getPrice());
        r.setQuantity(a.getQuantity());
        r.setUnit(a.getUnit());
        r.setStatus(a.getStatus());
        r.setLocation(a.getLocation());
        r.setPurchaseDate(a.getPurchaseDate());
        r.setWarrantyEnd(a.getWarrantyEnd());
        r.setDescription(a.getDescription());
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        return r;
    }
}
