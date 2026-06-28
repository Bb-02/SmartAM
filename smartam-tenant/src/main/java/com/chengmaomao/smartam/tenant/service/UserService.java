package com.chengmaomao.smartam.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chengmaomao.smartam.common.exception.BusinessException;
import com.chengmaomao.smartam.common.security.JwtUser;
import com.chengmaomao.smartam.tenant.dto.ChangePasswordRequest;
import com.chengmaomao.smartam.tenant.dto.UserCreateRequest;
import com.chengmaomao.smartam.tenant.dto.UserResponse;
import com.chengmaomao.smartam.tenant.dto.UserSelfUpdateRequest;
import com.chengmaomao.smartam.tenant.dto.UserUpdateRequest;
import com.chengmaomao.smartam.tenant.entity.Asset;
import com.chengmaomao.smartam.tenant.entity.AssetLog;
import com.chengmaomao.smartam.tenant.entity.AssetStatus;
import com.chengmaomao.smartam.tenant.entity.Department;
import com.chengmaomao.smartam.tenant.entity.Region;
import com.chengmaomao.smartam.tenant.entity.RoleEnum;
import com.chengmaomao.smartam.tenant.entity.User;
import com.chengmaomao.smartam.tenant.entity.WorkOrder;
import com.chengmaomao.smartam.tenant.entity.WorkOrderStatus;
import com.chengmaomao.smartam.tenant.mapper.AssetLogMapper;
import com.chengmaomao.smartam.tenant.mapper.AssetMapper;
import com.chengmaomao.smartam.tenant.mapper.DepartmentMapper;
import com.chengmaomao.smartam.tenant.mapper.RegionMapper;
import com.chengmaomao.smartam.tenant.mapper.UserMapper;
import com.chengmaomao.smartam.tenant.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RegionMapper regionMapper;
    private final AssetMapper assetMapper;
    private final AssetLogMapper assetLogMapper;
    private final WorkOrderMapper workOrderMapper;
    private final DepartmentMapper departmentMapper;

    private JwtUser currentUser() {
        return (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** 按角色注入数据范围过滤 */
    private void applyRoleFilter(LambdaQueryWrapper<User> qw, JwtUser user) {
        qw.eq(User::getTenantId, user.getTenantId());
        if (RoleEnum.ADMIN_REGION.equals(user.getRole())) {
            qw.eq(User::getRegionId, user.getRegionId());
        }
        // ADMIN_TENANT: only tenant_id
    }

    /** 检查当前用户是否有权操作该用户 */
    private User getOwnedUser(Long userId) {
        JwtUser me = currentUser();
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new BusinessException("用户不存在");
        }
        if (!target.getTenantId().equals(me.getTenantId())) {
            throw new BusinessException("用户不存在");
        }
        if (RoleEnum.ADMIN_REGION.equals(me.getRole())
                && !target.getRegionId().equals(me.getRegionId())) {
            throw new BusinessException("无权操作该用户");
        }
        return target;
    }

    @Transactional
    public UserResponse create(UserCreateRequest req) {
        JwtUser me = currentUser();
        if (!RoleEnum.ADMIN_REGION.equals(me.getRole()) && !RoleEnum.ADMIN_TENANT.equals(me.getRole())) {
            throw new BusinessException("无权限创建用户");
        }
        // ADMIN_TENANT 可创建 ADMIN_REGION / ENGINEER / EMPLOYEE
        // ADMIN_REGION 只能创建 ENGINEER / EMPLOYEE
        if (RoleEnum.ADMIN_TENANT.equals(me.getRole())) {
            if (RoleEnum.ADMIN_TENANT.equals(req.getRole())) {
                throw new BusinessException("不能创建租户管理员账号");
            }
            if (RoleEnum.ADMIN_REGION.equals(req.getRole()) && req.getRegionId() == null) {
                throw new BusinessException("创建分区管理员需指定所属分区");
            }
        } else {
            if (!RoleEnum.EMPLOYEE.equals(req.getRole()) && !RoleEnum.ENGINEER.equals(req.getRole())) {
                throw new BusinessException("只能创建员工或工程师账号");
            }
        }
        // EMPLOYEE 必须归属部门
        if (RoleEnum.EMPLOYEE.equals(req.getRole()) && req.getDeptId() == null) {
            throw new BusinessException("创建员工需指定所属部门");
        }

        // 检查 username 在租户内唯一
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, me.getTenantId())
                .eq(User::getUsername, req.getUsername()));
        if (count > 0) {
            throw new BusinessException("该账号已存在");
        }

        User user = new User();
        user.setTenantId(me.getTenantId());
        // ADMIN_REGION 强制用自己的 region；ADMIN_TENANT 可指定
        Long regionId = RoleEnum.ADMIN_REGION.equals(me.getRole())
                ? me.getRegionId()
                : (req.getRegionId() != null ? req.getRegionId() : me.getRegionId());
        Region region = regionMapper.selectById(regionId);
        if (region == null || !region.getTenantId().equals(me.getTenantId())) {
            throw new BusinessException("分区不存在");
        }
        user.setRegionId(regionId);
        user.setDeptId(req.getDeptId());
        if (req.getDeptId() != null) {
            Department dept = departmentMapper.selectById(req.getDeptId());
            if (dept == null || !dept.getTenantId().equals(me.getTenantId())) {
                throw new BusinessException("部门不存在");
            }
            if (!dept.getRegionId().equals(regionId)) {
                throw new BusinessException("部门不属于该分区");
            }
        }
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setRole(req.getRole());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setStatus(1);
        userMapper.insert(user);
        user = userMapper.selectById(user.getId());
        return toResponse(user);
    }

    public UserResponse getById(Long id) {
        return toResponse(getOwnedUser(id));
    }

    public UserResponse getMe() {
        return toResponse(getOwnedUser(currentUser().getUserId()));
    }

    @Transactional
    public UserResponse updateMe(UserSelfUpdateRequest req) {
        JwtUser me = currentUser();
        User user = getOwnedUser(me.getUserId());
        if (req.getRealName() != null) user.setRealName(req.getRealName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        userMapper.updateById(user);
        return toResponse(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        JwtUser me = currentUser();
        User user = getOwnedUser(me.getUserId());
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
    }

    public IPage<UserResponse> page(int page, int size, String role, Long regionId, Long deptId, String keyword) {
        JwtUser me = currentUser();
        if (!RoleEnum.ADMIN_REGION.equals(me.getRole()) && !RoleEnum.ADMIN_TENANT.equals(me.getRole())) {
            throw new BusinessException("无权限查看用户列表");
        }

        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        applyRoleFilter(qw, me);

        // ADMIN_TENANT 可额外按分区筛选
        if (regionId != null && RoleEnum.ADMIN_TENANT.equals(me.getRole())) {
            qw.eq(User::getRegionId, regionId);
        }
        if (StringUtils.hasText(role)) {
            qw.eq(User::getRole, role);
        }
        if (deptId != null) {
            qw.eq(User::getDeptId, deptId);
        }
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getPhone, keyword));
        }
        qw.orderByDesc(User::getId);

        Page<User> result = userMapper.selectPage(Page.of(page, size), qw);
        return result.convert(this::toResponse);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest req) {
        User target = getOwnedUser(id);

        if (req.getRealName() != null) target.setRealName(req.getRealName());
        if (req.getPhone() != null) target.setPhone(req.getPhone());
        if (req.getEmail() != null) target.setEmail(req.getEmail());
        if (req.getRegionId() != null) {
            JwtUser me = currentUser();
            if (RoleEnum.ADMIN_REGION.equals(me.getRole())) {
                throw new BusinessException("区域管理员无权变更用户分区");
            }
            Region region = regionMapper.selectById(req.getRegionId());
            if (region == null || !region.getTenantId().equals(target.getTenantId())) {
                throw new BusinessException("分区不存在");
            }
            target.setRegionId(req.getRegionId());
        }
        if (req.getDeptId() != null) {
            Department dept = departmentMapper.selectById(req.getDeptId());
            if (dept == null || !dept.getTenantId().equals(target.getTenantId())) {
                throw new BusinessException("部门不存在");
            }
            if (!dept.getRegionId().equals(target.getRegionId())) {
                throw new BusinessException("部门不属于该分区");
            }
            target.setDeptId(req.getDeptId());
        }
        // 分区变更后，重校现有部门是否属于新分区
        if (req.getRegionId() != null && target.getDeptId() != null) {
            Department dept = departmentMapper.selectById(target.getDeptId());
            if (dept == null || !dept.getTenantId().equals(target.getTenantId())) {
                throw new BusinessException("部门不存在");
            }
            if (!dept.getRegionId().equals(target.getRegionId())) {
                throw new BusinessException("该用户的部门不属于新分区，请先调整部门");
            }
        }
        // 换部门时，名下资产归还原部门
        if (req.getDeptId() != null) {
            List<Asset> userAssets = assetMapper.selectList(new LambdaQueryWrapper<Asset>()
                    .eq(Asset::getUserId, id));
            for (Asset asset : userAssets) {
                asset.setUserId(null);
                if (AssetStatus.IN_USE.equals(asset.getStatus())) {
                    asset.setStatus(AssetStatus.IN_STORAGE);
                }
                assetMapper.updateById(asset);
                AssetLog log = new AssetLog();
                log.setTenantId(asset.getTenantId());
                log.setAssetId(asset.getId());
                log.setOperatorId(currentUser().getUserId());
                log.setAction("RETURN");
                log.setDescription("用户 " + target.getRealName() + " 变更部门，资产归还");
                assetLogMapper.insert(log);
            }
        }

        if (req.getStatus() != null) {
            if (req.getStatus() != 0 && req.getStatus() != 1) {
                throw new BusinessException("无效的用户状态");
            }
            target.setStatus(req.getStatus());
        }
        if (StringUtils.hasText(req.getPassword())) {
            target.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        userMapper.updateById(target);
        return toResponse(target);
    }

    @Transactional
    public void delete(Long id) {
        User target = getOwnedUser(id);
        JwtUser me = currentUser();
        if (!RoleEnum.ADMIN_TENANT.equals(me.getRole())) {
            throw new BusinessException("仅租户管理员可删除用户");
        }
        if (RoleEnum.ADMIN_TENANT.equals(target.getRole())
                || RoleEnum.ADMIN_REGION.equals(target.getRole())) {
            throw new BusinessException("不能删除管理员账号");
        }

        Long assetCount = assetMapper.selectCount(new LambdaQueryWrapper<Asset>()
                .eq(Asset::getUserId, id));
        if (assetCount > 0) {
            throw new BusinessException("该用户名下还有资产，请先转移资产后再删除");
        }

        Long woCount = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .and(w -> w.eq(WorkOrder::getReporterId, id).or().eq(WorkOrder::getEngineerId, id))
                .notIn(WorkOrder::getStatus, WorkOrderStatus.CLOSED, WorkOrderStatus.CANCELLED));
        if (woCount > 0) {
            throw new BusinessException("该用户还有活跃工单，请先处理工单后再删除");
        }

        userMapper.deleteById(target.getId());
    }

    private UserResponse toResponse(User u) {
        UserResponse r = new UserResponse();
        r.setId(u.getId());
        r.setTenantId(u.getTenantId());
        r.setRegionId(u.getRegionId());
        r.setDeptId(u.getDeptId());
        r.setUsername(u.getUsername());
        r.setRealName(u.getRealName());
        r.setPhone(u.getPhone());
        r.setEmail(u.getEmail());
        r.setRole(u.getRole());
        r.setStatus(u.getStatus());
        r.setCreatedAt(u.getCreatedAt());
        r.setUpdatedAt(u.getUpdatedAt());
        return r;
    }
}
