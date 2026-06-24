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
import com.chengmaomao.smartam.tenant.entity.AssetStatus;
import com.chengmaomao.smartam.tenant.entity.RoleEnum;
import com.chengmaomao.smartam.tenant.mapper.AssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetMapper assetMapper;

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

        Asset asset = new Asset();
        asset.setTenantId(user.getTenantId());
        // ADMIN_TENANT 可指定分区，否则用自身分区
        if (RoleEnum.ADMIN_TENANT.equals(user.getRole()) && req.getRegionId() != null) {
            asset.setRegionId(req.getRegionId());
        } else {
            asset.setRegionId(user.getRegionId());
        }
        asset.setDeptId(req.getDeptId());
        asset.setUserId(req.getUserId());
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
                                     Long deptId, String keyword) {
        JwtUser user = currentUser();
        LambdaQueryWrapper<Asset> qw = new LambdaQueryWrapper<>();
        applyRoleFilter(qw, user);

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
        Asset asset = getOwnedAsset(id);
        JwtUser user = currentUser();
        if (RoleEnum.EMPLOYEE.equals(user.getRole()) || RoleEnum.ENGINEER.equals(user.getRole())) {
            throw new BusinessException("无权限编辑资产");
        }

        if (req.getDeptId() != null) asset.setDeptId(req.getDeptId());
        if (req.getUserId() != null) asset.setUserId(req.getUserId());
        if (req.getName() != null) asset.setName(req.getName());
        if (req.getCode() != null) asset.setCode(req.getCode());
        if (req.getCategory() != null) asset.setCategory(req.getCategory());
        if (req.getModel() != null) asset.setModel(req.getModel());
        if (req.getBrand() != null) asset.setBrand(req.getBrand());
        if (req.getPrice() != null) asset.setPrice(req.getPrice());
        if (req.getQuantity() != null) asset.setQuantity(req.getQuantity());
        if (req.getUnit() != null) asset.setUnit(req.getUnit());
        if (req.getStatus() != null) {
            if (!AssetStatus.isValid(req.getStatus())) {
                throw new BusinessException("无效的资产状态: " + req.getStatus());
            }
            asset.setStatus(req.getStatus());
        }
        if (req.getLocation() != null) asset.setLocation(req.getLocation());
        if (req.getPurchaseDate() != null) asset.setPurchaseDate(req.getPurchaseDate());
        if (req.getWarrantyEnd() != null) asset.setWarrantyEnd(req.getWarrantyEnd());
        if (req.getDescription() != null) asset.setDescription(req.getDescription());

        assetMapper.updateById(asset);
        return toResponse(asset);
    }

    @Transactional
    public void delete(Long id) {
        Asset asset = getOwnedAsset(id);
        JwtUser user = currentUser();
        if (!RoleEnum.ADMIN_TENANT.equals(user.getRole())) {
            throw new BusinessException("仅租户管理员可删除资产");
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
