package com.chengmaomao.smartam.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chengmaomao.smartam.common.exception.BusinessException;
import com.chengmaomao.smartam.common.util.JwtUtil;
import com.chengmaomao.smartam.tenant.dto.LoginRequest;
import com.chengmaomao.smartam.tenant.dto.LoginResponse;
import com.chengmaomao.smartam.tenant.entity.Region;
import com.chengmaomao.smartam.tenant.entity.Tenant;
import com.chengmaomao.smartam.tenant.entity.User;
import com.chengmaomao.smartam.tenant.mapper.RegionMapper;
import com.chengmaomao.smartam.tenant.mapper.TenantMapper;
import com.chengmaomao.smartam.tenant.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantMapper tenantMapper;
    private final RegionMapper regionMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest req) {
        // 1. 查公司
        Tenant tenant = tenantMapper.selectOne(
                new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, req.getCompanyCode()));
        if (tenant == null) {
            throw new BusinessException("公司不存在");
        }

        // 2. 查用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, tenant.getId())
                .eq(User::getUsername, req.getUsername()));
        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }

        // 3. 验密
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }

        // 4. 检查状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 5. 签发 JWT
        String token = JwtUtil.generate(
                user.getId(), tenant.getId(), user.getRegionId(), user.getDeptId(),
                user.getUsername(), user.getRole());

        // 6. 查分区名称
        Region region = regionMapper.selectById(user.getRegionId());
        String regionName = region != null ? region.getName() : "";

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole(), user.getRealName(),
                tenant.getName(), user.getRegionId(), regionName, user.getDeptId());
    }
}
