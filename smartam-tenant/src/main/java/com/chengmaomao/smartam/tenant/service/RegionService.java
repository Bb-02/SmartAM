package com.chengmaomao.smartam.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chengmaomao.smartam.common.exception.BusinessException;
import com.chengmaomao.smartam.common.security.JwtUser;
import com.chengmaomao.smartam.tenant.dto.RegionCreateRequest;
import com.chengmaomao.smartam.tenant.dto.RegionResponse;
import com.chengmaomao.smartam.tenant.dto.RegionUpdateRequest;
import com.chengmaomao.smartam.tenant.entity.Region;
import com.chengmaomao.smartam.tenant.entity.RoleEnum;
import com.chengmaomao.smartam.tenant.entity.User;
import com.chengmaomao.smartam.tenant.entity.Asset;
import com.chengmaomao.smartam.tenant.entity.Department;
import com.chengmaomao.smartam.tenant.mapper.AssetMapper;
import com.chengmaomao.smartam.tenant.mapper.DepartmentMapper;
import com.chengmaomao.smartam.tenant.mapper.RegionMapper;
import com.chengmaomao.smartam.tenant.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionMapper regionMapper;
    private final UserMapper userMapper;
    private final AssetMapper assetMapper;
    private final DepartmentMapper departmentMapper;

    private JwtUser currentUser() {
        return (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Region getOwnedRegion(Long regionId) {
        JwtUser me = currentUser();
        Region region = regionMapper.selectById(regionId);
        if (region == null || !region.getTenantId().equals(me.getTenantId())) {
            throw new BusinessException("分区不存在");
        }
        if (RoleEnum.ADMIN_REGION.equals(me.getRole())
                && !region.getId().equals(me.getRegionId())) {
            throw new BusinessException("无权查看该分区");
        }
        return region;
    }

    private void checkTenantAdmin() {
        if (!RoleEnum.ADMIN_TENANT.equals(currentUser().getRole())) {
            throw new BusinessException("仅租户管理员可操作分区");
        }
    }

    @Transactional
    public RegionResponse create(RegionCreateRequest req) {
        checkTenantAdmin();
        JwtUser me = currentUser();

        Long count = regionMapper.selectCount(new LambdaQueryWrapper<Region>()
                .eq(Region::getTenantId, me.getTenantId())
                .eq(Region::getCode, req.getCode()));
        if (count > 0) {
            throw new BusinessException("该分区标识已存在");
        }

        Region region = new Region();
        region.setTenantId(me.getTenantId());
        region.setName(req.getName());
        region.setCode(req.getCode());
        region.setIsDefault(0);
        region.setStatus(1);
        regionMapper.insert(region);
        region = regionMapper.selectById(region.getId());
        return toResponse(region);
    }

    public RegionResponse getById(Long id) {
        return toResponse(getOwnedRegion(id));
    }

    public IPage<RegionResponse> page(int page, int size, String keyword) {
        JwtUser me = currentUser();

        LambdaQueryWrapper<Region> qw = new LambdaQueryWrapper<>();
        qw.eq(Region::getTenantId, me.getTenantId());
        if (RoleEnum.ADMIN_REGION.equals(me.getRole())) {
            qw.eq(Region::getId, me.getRegionId());
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Region::getName, keyword).or().like(Region::getCode, keyword));
        }
        qw.orderByAsc(Region::getIsDefault).orderByDesc(Region::getId);

        Page<Region> result = regionMapper.selectPage(Page.of(page, size), qw);
        return result.convert(this::toResponse);
    }

    @Transactional
    public RegionResponse update(Long id, RegionUpdateRequest req) {
        checkTenantAdmin();
        Region region = getOwnedRegion(id);

        if (req.getName() != null) region.setName(req.getName());
        if (req.getCode() != null) {
            Long count = regionMapper.selectCount(new LambdaQueryWrapper<Region>()
                    .eq(Region::getTenantId, region.getTenantId())
                    .eq(Region::getCode, req.getCode())
                    .ne(Region::getId, id));
            if (count > 0) {
                throw new BusinessException("该分区标识已存在");
            }
            region.setCode(req.getCode());
        }

        regionMapper.updateById(region);
        return toResponse(region);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        checkTenantAdmin();
        Region region = getOwnedRegion(id);
        if (region.getIsDefault() == 1) {
            throw new BusinessException("不能冻结/停用默认分区");
        }
        region.setStatus(status);
        regionMapper.updateById(region);
    }

    @Transactional
    public void delete(Long id) {
        checkTenantAdmin();
        Region region = getOwnedRegion(id);
        if (region.getIsDefault() == 1) {
            throw new BusinessException("不能删除默认分区");
        }

        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRegionId, id));
        if (userCount > 0) {
            throw new BusinessException("该分区下存在用户，请先迁移用户后再删除");
        }

        Long assetCount = assetMapper.selectCount(new LambdaQueryWrapper<Asset>()
                .eq(Asset::getRegionId, id));
        if (assetCount > 0) {
            throw new BusinessException("该分区下存在资产，请先迁移资产后再删除");
        }

        Long deptCount = departmentMapper.selectCount(new LambdaQueryWrapper<Department>()
                .eq(Department::getRegionId, id));
        if (deptCount > 0) {
            throw new BusinessException("该分区下存在部门，请先删除部门后再删除");
        }

        regionMapper.deleteById(id);
    }

    private RegionResponse toResponse(Region r) {
        RegionResponse resp = new RegionResponse();
        resp.setId(r.getId());
        resp.setTenantId(r.getTenantId());
        resp.setName(r.getName());
        resp.setCode(r.getCode());
        resp.setIsDefault(r.getIsDefault());
        resp.setStatus(r.getStatus());
        resp.setCreatedAt(r.getCreatedAt());
        resp.setUpdatedAt(r.getUpdatedAt());
        return resp;
    }
}
