package com.chengmaomao.smartam.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chengmaomao.smartam.common.exception.BusinessException;
import com.chengmaomao.smartam.tenant.dto.RegisterTenantRequest;
import com.chengmaomao.smartam.tenant.entity.Region;
import com.chengmaomao.smartam.tenant.entity.RoleEnum;
import com.chengmaomao.smartam.tenant.entity.Tenant;
import com.chengmaomao.smartam.tenant.entity.User;
import com.chengmaomao.smartam.tenant.mapper.RegionMapper;
import com.chengmaomao.smartam.tenant.mapper.TenantMapper;
import com.chengmaomao.smartam.tenant.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;
    private final RegionMapper regionMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Tenant register(RegisterTenantRequest req) {
        // 1. 校验公司标识唯一
        Long count = tenantMapper.selectCount(
                new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, req.getCompanyCode()));
        if (count > 0) {
            throw new BusinessException("该公司标识已被注册");
        }

        // 2. 创建租户
        Tenant tenant = new Tenant();
        tenant.setName(req.getCompanyName());
        tenant.setCode(req.getCompanyCode());
        tenant.setStatus(1);
        tenantMapper.insert(tenant);

        // 3. 创建默认总分区
        Region region = new Region();
        region.setTenantId(tenant.getId());
        region.setName("总分区");
        region.setCode("default");
        region.setIsDefault(1);
        region.setStatus(1);
        regionMapper.insert(region);

        // 4. 创建 ADMIN_TENANT 管理员
        User admin = new User();
        admin.setTenantId(tenant.getId());
        admin.setRegionId(region.getId());
        admin.setUsername(req.getAdminUsername());
        admin.setPassword(passwordEncoder.encode(req.getPassword()));
        admin.setRealName(req.getRealName());
        admin.setRole(RoleEnum.ADMIN_TENANT);
        admin.setStatus(1);
        userMapper.insert(admin);

        return tenant;
    }
}
