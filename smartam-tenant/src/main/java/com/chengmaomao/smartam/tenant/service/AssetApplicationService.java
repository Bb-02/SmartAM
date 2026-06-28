package com.chengmaomao.smartam.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chengmaomao.smartam.common.exception.BusinessException;
import com.chengmaomao.smartam.common.security.JwtUser;
import com.chengmaomao.smartam.tenant.dto.AssetApplicationCreateRequest;
import com.chengmaomao.smartam.tenant.dto.AssetApplicationResponse;
import com.chengmaomao.smartam.tenant.entity.Asset;
import com.chengmaomao.smartam.tenant.entity.AssetApplication;
import com.chengmaomao.smartam.tenant.entity.AssetApplicationStatus;
import com.chengmaomao.smartam.tenant.entity.AssetStatus;
import com.chengmaomao.smartam.tenant.entity.RoleEnum;
import com.chengmaomao.smartam.tenant.entity.User;
import com.chengmaomao.smartam.tenant.entity.AssetApplicationLog;
import com.chengmaomao.smartam.tenant.entity.AssetLog;
import com.chengmaomao.smartam.tenant.mapper.AssetApplicationLogMapper;
import com.chengmaomao.smartam.tenant.mapper.AssetLogMapper;
import com.chengmaomao.smartam.tenant.mapper.AssetApplicationMapper;
import com.chengmaomao.smartam.tenant.mapper.AssetMapper;
import com.chengmaomao.smartam.tenant.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssetApplicationService {

    private final AssetApplicationMapper applicationMapper;
    private final AssetMapper assetMapper;
    private final UserMapper userMapper;
    private final AssetApplicationLogMapper applicationLogMapper;
    private final AssetLogMapper assetLogMapper;

    private JwtUser currentUser() {
        return (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional
    public AssetApplicationResponse create(AssetApplicationCreateRequest req) {
        JwtUser me = currentUser();
        if (!RoleEnum.EMPLOYEE.equals(me.getRole())) {
            throw new BusinessException("仅员工可提交申领");
        }

        Asset asset = assetMapper.selectById(req.getAssetId());
        if (asset == null || !asset.getTenantId().equals(me.getTenantId())) {
            throw new BusinessException("资产不存在");
        }
        if (!AssetStatus.IN_STORAGE.equals(asset.getStatus())) {
            throw new BusinessException("该资产不可申领，仅库存在库资产可申领");
        }

        // 防止重复申领
        Long dup = applicationMapper.selectCount(new LambdaQueryWrapper<AssetApplication>()
                .eq(AssetApplication::getAssetId, req.getAssetId())
                .eq(AssetApplication::getApplicantId, me.getUserId())
                .eq(AssetApplication::getStatus, AssetApplicationStatus.PENDING));
        if (dup > 0) {
            throw new BusinessException("您已对该资产提交过申领，请等待审批");
        }

        AssetApplication app = new AssetApplication();
        app.setTenantId(me.getTenantId());
        app.setRegionId(asset.getRegionId());
        app.setAssetId(req.getAssetId());
        app.setApplicantId(me.getUserId());
        app.setReason(req.getReason());
        app.setStatus(AssetApplicationStatus.PENDING);
        applicationMapper.insert(app);
        app = applicationMapper.selectById(app.getId());
        return toResponse(app);
    }

    public AssetApplicationResponse getById(Long id) {
        return toResponse(getOwned(id));
    }

    public IPage<AssetApplicationResponse> page(int page, int size, String status) {
        JwtUser me = currentUser();

        LambdaQueryWrapper<AssetApplication> qw = new LambdaQueryWrapper<>();
        qw.eq(AssetApplication::getTenantId, me.getTenantId());

        if (RoleEnum.EMPLOYEE.equals(me.getRole())) {
            qw.eq(AssetApplication::getApplicantId, me.getUserId());
        }
        if (RoleEnum.ADMIN_REGION.equals(me.getRole())) {
            qw.eq(AssetApplication::getRegionId, me.getRegionId());
        }
        if (status != null && !status.isBlank()) {
            qw.eq(AssetApplication::getStatus, status);
        }
        qw.orderByDesc(AssetApplication::getId);

        Page<AssetApplication> result = applicationMapper.selectPage(Page.of(page, size), qw);
        return result.convert(this::toResponse);
    }

    @Transactional
    public AssetApplicationResponse approve(Long id) {
        JwtUser me = currentUser();
        if (!RoleEnum.ADMIN_REGION.equals(me.getRole()) && !RoleEnum.ADMIN_TENANT.equals(me.getRole())) {
            throw new BusinessException("无权限审批");
        }

        AssetApplication app = getOwned(id);
        if (!AssetApplicationStatus.PENDING.equals(app.getStatus())) {
            throw new BusinessException("该申请已被处理");
        }

        Asset asset = assetMapper.selectById(app.getAssetId());
        if (asset == null || !asset.getTenantId().equals(me.getTenantId())) {
            throw new BusinessException("资产不存在");
        }
        if (!AssetStatus.IN_STORAGE.equals(asset.getStatus())) {
            throw new BusinessException("该资产已被分配，无法重复审批");
        }

        // 自动分配：asset 归属申请人
        User applicant = userMapper.selectById(app.getApplicantId());
        asset.setUserId(app.getApplicantId());
        if (applicant != null && applicant.getDeptId() != null) {
            asset.setDeptId(applicant.getDeptId());
        }
        asset.setStatus(AssetStatus.IN_USE);
        assetMapper.updateById(asset);

        writeAssetLog(asset, me.getUserId(), "ASSIGN",
                "申领审批通过，分配给 " + (applicant != null ? applicant.getRealName() : "用户" + app.getApplicantId()));

        app.setStatus(AssetApplicationStatus.APPROVED);
        app.setApproverId(me.getUserId());
        applicationMapper.updateById(app);

        writeLog(app.getId(), app.getAssetId(), AssetApplicationStatus.PENDING, AssetApplicationStatus.APPROVED, me.getUserId(), null);
        return toResponse(app);
    }

    @Transactional
    public AssetApplicationResponse reject(Long id, String remark) {
        JwtUser me = currentUser();
        if (!RoleEnum.ADMIN_REGION.equals(me.getRole()) && !RoleEnum.ADMIN_TENANT.equals(me.getRole())) {
            throw new BusinessException("无权限审批");
        }

        AssetApplication app = getOwned(id);
        if (!AssetApplicationStatus.PENDING.equals(app.getStatus())) {
            throw new BusinessException("该申请已被处理");
        }

        app.setStatus(AssetApplicationStatus.REJECTED);
        app.setApproverId(me.getUserId());
        app.setRemark(remark);
        applicationMapper.updateById(app);

        writeLog(app.getId(), app.getAssetId(), AssetApplicationStatus.PENDING, AssetApplicationStatus.REJECTED, me.getUserId(), remark);
        return toResponse(app);
    }

    @Transactional
    public AssetApplicationResponse cancel(Long id) {
        JwtUser me = currentUser();
        AssetApplication app = getOwned(id);

        if (!AssetApplicationStatus.PENDING.equals(app.getStatus())) {
            throw new BusinessException("仅待审批状态的申请可取消");
        }
        if (!me.getUserId().equals(app.getApplicantId())) {
            throw new BusinessException("仅申请人可取消");
        }

        app.setStatus(AssetApplicationStatus.CANCELLED);
        applicationMapper.updateById(app);

        writeLog(app.getId(), app.getAssetId(), AssetApplicationStatus.PENDING, AssetApplicationStatus.CANCELLED, me.getUserId(), "申请人取消");
        return toResponse(app);
    }

    private AssetApplication getOwned(Long id) {
        JwtUser me = currentUser();
        AssetApplication app = applicationMapper.selectById(id);
        if (app == null || !app.getTenantId().equals(me.getTenantId())) {
            throw new BusinessException("申领记录不存在");
        }
        if (RoleEnum.ADMIN_REGION.equals(me.getRole())
                && !app.getRegionId().equals(me.getRegionId())) {
            throw new BusinessException("无权查看该申领");
        }
        if (RoleEnum.EMPLOYEE.equals(me.getRole())
                && !app.getApplicantId().equals(me.getUserId())) {
            throw new BusinessException("无权查看该申领");
        }
        return app;
    }

    private final Map<Long, String> nameCache = new HashMap<>();

    private String getUserName(Long userId) {
        if (userId == null) return null;
        return nameCache.computeIfAbsent(userId, id -> {
            User u = userMapper.selectById(id);
            return u != null ? u.getRealName() : null;
        });
    }

    private AssetApplicationResponse toResponse(AssetApplication a) {
        nameCache.clear();
        AssetApplicationResponse r = new AssetApplicationResponse();
        r.setId(a.getId());
        r.setTenantId(a.getTenantId());
        r.setRegionId(a.getRegionId());
        r.setAssetId(a.getAssetId());
        r.setAssetName(getAssetName(a.getAssetId()));
        r.setApplicantId(a.getApplicantId());
        r.setApplicantName(getUserName(a.getApplicantId()));
        r.setReason(a.getReason());
        r.setStatus(a.getStatus());
        r.setApproverId(a.getApproverId());
        r.setApproverName(getUserName(a.getApproverId()));
        r.setRemark(a.getRemark());
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        return r;
    }

    private String getAssetName(Long assetId) {
        if (assetId == null) return null;
        Asset asset = assetMapper.selectById(assetId);
        return asset != null ? asset.getName() : null;
    }

    private void writeLog(Long applicationId, Long assetId, String fromStatus, String toStatus,
                          Long operatorId, String remark) {
        AssetApplicationLog log = new AssetApplicationLog();
        log.setApplicationId(applicationId);
        log.setAssetId(assetId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        applicationLogMapper.insert(log);
    }

    private void writeAssetLog(Asset asset, Long operatorId, String action, String description) {
        AssetLog log = new AssetLog();
        log.setTenantId(asset.getTenantId());
        log.setAssetId(asset.getId());
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setDescription(description);
        assetLogMapper.insert(log);
    }
}
