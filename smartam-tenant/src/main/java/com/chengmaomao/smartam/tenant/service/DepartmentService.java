package com.chengmaomao.smartam.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chengmaomao.smartam.common.exception.BusinessException;
import com.chengmaomao.smartam.common.security.JwtUser;
import com.chengmaomao.smartam.tenant.dto.DepartmentCreateRequest;
import com.chengmaomao.smartam.tenant.dto.DepartmentResponse;
import com.chengmaomao.smartam.tenant.dto.DepartmentUpdateRequest;
import com.chengmaomao.smartam.tenant.entity.Asset;
import com.chengmaomao.smartam.tenant.entity.Department;
import com.chengmaomao.smartam.tenant.entity.Region;
import com.chengmaomao.smartam.tenant.entity.RoleEnum;
import com.chengmaomao.smartam.tenant.entity.User;
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
public class DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;
    private final AssetMapper assetMapper;
    private final RegionMapper regionMapper;

    private JwtUser currentUser() {
        return (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Department getOwnedDept(Long deptId) {
        JwtUser me = currentUser();
        Department dept = departmentMapper.selectById(deptId);
        if (dept == null || !dept.getTenantId().equals(me.getTenantId())) {
            throw new BusinessException("部门不存在");
        }
        if (RoleEnum.ADMIN_REGION.equals(me.getRole())
                && !dept.getRegionId().equals(me.getRegionId())) {
            throw new BusinessException("无权操作该部门");
        }
        return dept;
    }

    /** ADMIN_REGION / ADMIN_TENANT 均可操作 */
    private void checkAdmin() {
        JwtUser me = currentUser();
        if (!RoleEnum.ADMIN_REGION.equals(me.getRole()) && !RoleEnum.ADMIN_TENANT.equals(me.getRole())) {
            throw new BusinessException("无权限操作部门");
        }
    }

    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest req) {
        checkAdmin();
        JwtUser me = currentUser();

        // ADMIN_REGION 强制自己的分区；ADMIN_TENANT 必须指定
        Long regionId;
        if (RoleEnum.ADMIN_REGION.equals(me.getRole())) {
            regionId = me.getRegionId();
        } else {
            if (req.getRegionId() == null) {
                throw new BusinessException("请指定所属分区");
            }
            regionId = req.getRegionId();
        }
        Region region = regionMapper.selectById(regionId);
        if (region == null || !region.getTenantId().equals(me.getTenantId())) {
            throw new BusinessException("分区不存在");
        }

        // 父部门校验
        if (req.getParentId() != null) {
            Department parent = departmentMapper.selectById(req.getParentId());
            if (parent == null || !parent.getTenantId().equals(me.getTenantId())) {
                throw new BusinessException("父部门不存在");
            }
            if (!parent.getRegionId().equals(regionId)) {
                throw new BusinessException("父部门不属于该分区");
            }
        }

        // 编码唯一性
        Long codeCount = departmentMapper.selectCount(new LambdaQueryWrapper<Department>()
                .eq(Department::getTenantId, me.getTenantId())
                .eq(Department::getCode, req.getCode()));
        if (codeCount > 0) {
            throw new BusinessException("该部门编码已存在");
        }

        Department dept = new Department();
        dept.setTenantId(me.getTenantId());
        dept.setRegionId(regionId);
        dept.setParentId(req.getParentId());
        dept.setName(req.getName());
        dept.setCode(req.getCode());
        dept.setStatus(1);
        departmentMapper.insert(dept);
        dept = departmentMapper.selectById(dept.getId());
        return toResponse(dept);
    }

    public DepartmentResponse getById(Long id) {
        return toResponse(getOwnedDept(id));
    }

    public IPage<DepartmentResponse> page(int page, int size, Long regionId, Long parentId, String keyword) {
        checkAdmin();
        JwtUser me = currentUser();

        LambdaQueryWrapper<Department> qw = new LambdaQueryWrapper<>();
        qw.eq(Department::getTenantId, me.getTenantId());
        if (RoleEnum.ADMIN_REGION.equals(me.getRole())) {
            qw.eq(Department::getRegionId, me.getRegionId());
        } else if (regionId != null) {
            qw.eq(Department::getRegionId, regionId);
        }
        if (parentId != null) {
            qw.eq(Department::getParentId, parentId);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Department::getName, keyword).or().like(Department::getCode, keyword));
        }
        qw.orderByAsc(Department::getParentId).orderByDesc(Department::getId);

        Page<Department> result = departmentMapper.selectPage(Page.of(page, size), qw);
        return result.convert(this::toResponse);
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentUpdateRequest req) {
        checkAdmin();
        Department dept = getOwnedDept(id);

        if (req.getName() != null) dept.setName(req.getName());
        if (req.getCode() != null) {
            Long codeCount = departmentMapper.selectCount(new LambdaQueryWrapper<Department>()
                    .eq(Department::getTenantId, dept.getTenantId())
                    .eq(Department::getCode, req.getCode())
                    .ne(Department::getId, id));
            if (codeCount > 0) {
                throw new BusinessException("该部门编码已存在");
            }
            dept.setCode(req.getCode());
        }
        if (req.getParentId() != null) {
            if (req.getParentId().equals(id)) {
                throw new BusinessException("不能将自己设为父部门");
            }
            Department parent = departmentMapper.selectById(req.getParentId());
            if (parent == null || !parent.getTenantId().equals(dept.getTenantId())) {
                throw new BusinessException("父部门不存在");
            }
            if (!parent.getRegionId().equals(dept.getRegionId())) {
                throw new BusinessException("父部门不属于该分区");
            }
            dept.setParentId(req.getParentId());
        }

        departmentMapper.updateById(dept);
        return toResponse(dept);
    }

    @Transactional
    public void delete(Long id) {
        Department dept = getOwnedDept(id);
        JwtUser me = currentUser();
        if (!RoleEnum.ADMIN_TENANT.equals(me.getRole())) {
            throw new BusinessException("仅租户管理员可删除部门");
        }

        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getDeptId, id));
        if (userCount > 0) {
            throw new BusinessException("该部门下存在用户，请先迁移用户后再删除");
        }

        // 检查是否有子部门
        Long childCount = departmentMapper.selectCount(new LambdaQueryWrapper<Department>()
                .eq(Department::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("该部门下存在子部门，请先删除子部门");
        }

        Long assetCount = assetMapper.selectCount(new LambdaQueryWrapper<Asset>()
                .eq(Asset::getDeptId, id));
        if (assetCount > 0) {
            throw new BusinessException("该部门下存在资产，请先迁移资产后再删除");
        }

        departmentMapper.deleteById(id);
    }

    private DepartmentResponse toResponse(Department d) {
        DepartmentResponse r = new DepartmentResponse();
        r.setId(d.getId());
        r.setTenantId(d.getTenantId());
        r.setRegionId(d.getRegionId());
        r.setParentId(d.getParentId());
        r.setName(d.getName());
        r.setCode(d.getCode());
        r.setStatus(d.getStatus());
        r.setCreatedAt(d.getCreatedAt());
        r.setUpdatedAt(d.getUpdatedAt());
        return r;
    }
}
