package com.chengmaomao.smartam.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chengmaomao.smartam.common.exception.BusinessException;
import com.chengmaomao.smartam.common.security.JwtUser;
import com.chengmaomao.smartam.tenant.dto.WorkOrderConfirmRequest;
import com.chengmaomao.smartam.tenant.dto.WorkOrderCreateRequest;
import com.chengmaomao.smartam.tenant.dto.WorkOrderResolveRequest;
import com.chengmaomao.smartam.tenant.dto.WorkOrderResponse;
import com.chengmaomao.smartam.tenant.entity.RoleEnum;
import com.chengmaomao.smartam.tenant.entity.User;
import com.chengmaomao.smartam.tenant.entity.WorkOrder;
import com.chengmaomao.smartam.tenant.entity.WorkOrderLog;
import com.chengmaomao.smartam.tenant.entity.WorkOrderStatus;
import com.chengmaomao.smartam.tenant.mapper.UserMapper;
import com.chengmaomao.smartam.tenant.mapper.WorkOrderLogMapper;
import com.chengmaomao.smartam.tenant.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderLogMapper workOrderLogMapper;
    private final UserMapper userMapper;

    private JwtUser currentUser() {
        return (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** 按角色注入数据范围过滤 */
    private void applyRoleFilter(LambdaQueryWrapper<WorkOrder> qw, JwtUser user) {
        qw.eq(WorkOrder::getTenantId, user.getTenantId());
        switch (user.getRole()) {
            case RoleEnum.EMPLOYEE:
                qw.eq(WorkOrder::getReporterId, user.getUserId());
                break;
            case RoleEnum.ENGINEER:
                qw.eq(WorkOrder::getRegionId, user.getRegionId());
                break;
            case RoleEnum.ADMIN_REGION:
                qw.eq(WorkOrder::getRegionId, user.getRegionId());
                break;
            // ADMIN_TENANT: only tenant_id
        }
    }

    /** 获取有权限查看的工单 */
    private WorkOrder getOwnedWorkOrder(Long id) {
        JwtUser me = currentUser();
        WorkOrder wo = workOrderMapper.selectById(id);
        if (wo == null || !wo.getTenantId().equals(me.getTenantId())) {
            throw new BusinessException("工单不存在");
        }
        if (RoleEnum.ADMIN_TENANT.equals(me.getRole())) {
            return wo;
        }
        if (RoleEnum.EMPLOYEE.equals(me.getRole())) {
            if (!wo.getReporterId().equals(me.getUserId())) {
                throw new BusinessException("无权查看该工单");
            }
            return wo;
        }
        // ENGINEER / ADMIN_REGION: same region
        if (!wo.getRegionId().equals(me.getRegionId())) {
            throw new BusinessException("无权查看该工单");
        }
        return wo;
    }

    @Transactional
    public WorkOrderResponse create(WorkOrderCreateRequest req) {
        JwtUser me = currentUser();
        if (!RoleEnum.EMPLOYEE.equals(me.getRole())) {
            throw new BusinessException("仅员工可提交工单");
        }

        WorkOrder wo = new WorkOrder();
        wo.setTenantId(me.getTenantId());
        wo.setRegionId(me.getRegionId());
        wo.setType(req.getType() != null ? req.getType() : "REPAIR");
        wo.setTitle(req.getTitle());
        wo.setDescription(req.getDescription());
        wo.setAssetId(req.getAssetId());
        wo.setReporterId(me.getUserId());
        wo.setPriority(req.getPriority() != null ? req.getPriority() : "NORMAL");
        wo.setStatus(WorkOrderStatus.PENDING);
        workOrderMapper.insert(wo);

        writeLog(wo.getId(), null, WorkOrderStatus.PENDING, me.getUserId(), null);
        return toResponse(wo);
    }

    public WorkOrderResponse getById(Long id) {
        return toResponse(getOwnedWorkOrder(id));
    }

    public IPage<WorkOrderResponse> page(int page, int size, String status, String priority) {
        JwtUser me = currentUser();
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
        applyRoleFilter(qw, me);

        if (status != null && !status.isBlank()) {
            qw.eq(WorkOrder::getStatus, status);
        }
        if (priority != null && !priority.isBlank()) {
            qw.eq(WorkOrder::getPriority, priority);
        }
        qw.orderByDesc(WorkOrder::getId);

        Page<WorkOrder> result = workOrderMapper.selectPage(Page.of(page, size), qw);
        return result.convert(this::toResponse);
    }

    @Transactional
    public WorkOrderResponse claim(Long id) {
        JwtUser me = currentUser();
        if (!RoleEnum.ENGINEER.equals(me.getRole())) {
            throw new BusinessException("仅工程师可受理工单");
        }

        WorkOrder wo = getOwnedWorkOrder(id);
        if (!WorkOrderStatus.PENDING.equals(wo.getStatus())) {
            throw new BusinessException("该工单已被受理或已关闭");
        }

        wo.setEngineerId(me.getUserId());
        wo.setStatus(WorkOrderStatus.IN_WORK);
        workOrderMapper.updateById(wo);

        writeLog(wo.getId(), WorkOrderStatus.PENDING, WorkOrderStatus.IN_WORK, me.getUserId(), null);
        return toResponse(wo);
    }

    @Transactional
    public WorkOrderResponse resolve(Long id, WorkOrderResolveRequest req) {
        JwtUser me = currentUser();
        WorkOrder wo = getOwnedWorkOrder(id);

        if (!WorkOrderStatus.IN_WORK.equals(wo.getStatus())) {
            throw new BusinessException("工单状态不正确");
        }
        if (!me.getUserId().equals(wo.getEngineerId())) {
            throw new BusinessException("仅受理工程师可提交处理结果");
        }

        wo.setStatus(WorkOrderStatus.RESOLVED);
        wo.setResolution(req.getResolution());
        workOrderMapper.updateById(wo);

        writeLog(wo.getId(), WorkOrderStatus.IN_WORK, WorkOrderStatus.RESOLVED, me.getUserId(), req.getResolution());
        return toResponse(wo);
    }

    @Transactional
    public WorkOrderResponse confirm(Long id, WorkOrderConfirmRequest req) {
        JwtUser me = currentUser();
        WorkOrder wo = getOwnedWorkOrder(id);

        if (!WorkOrderStatus.RESOLVED.equals(wo.getStatus())) {
            throw new BusinessException("工单状态不正确");
        }
        if (!me.getUserId().equals(wo.getReporterId())) {
            throw new BusinessException("仅报修人可确认结单");
        }

        wo.setStatus(WorkOrderStatus.CLOSED);
        wo.setRating(req.getRating());
        workOrderMapper.updateById(wo);

        writeLog(wo.getId(), WorkOrderStatus.RESOLVED, WorkOrderStatus.CLOSED, me.getUserId(),
                "评分: " + req.getRating());
        return toResponse(wo);
    }

    @Transactional
    public WorkOrderResponse reject(Long id, String remark) {
        JwtUser me = currentUser();
        WorkOrder wo = getOwnedWorkOrder(id);

        if (!WorkOrderStatus.RESOLVED.equals(wo.getStatus())) {
            throw new BusinessException("工单状态不正确");
        }
        if (!me.getUserId().equals(wo.getReporterId())) {
            throw new BusinessException("仅报修人可驳回");
        }

        wo.setStatus(WorkOrderStatus.IN_WORK);
        workOrderMapper.updateById(wo);

        writeLog(wo.getId(), WorkOrderStatus.RESOLVED, WorkOrderStatus.IN_WORK, me.getUserId(), remark);
        return toResponse(wo);
    }

    public List<WorkOrderLog> getLogs(Long id) {
        getOwnedWorkOrder(id);
        return workOrderLogMapper.selectList(new LambdaQueryWrapper<WorkOrderLog>()
                .eq(WorkOrderLog::getWorkOrderId, id)
                .orderByAsc(WorkOrderLog::getCreatedAt));
    }

    private void writeLog(Long workOrderId, String fromStatus, String toStatus,
                          Long operatorId, String remark) {
        WorkOrderLog log = new WorkOrderLog();
        log.setWorkOrderId(workOrderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        workOrderLogMapper.insert(log);
    }

    private final Map<Long, String> nameCache = new HashMap<>();

    private String getUserName(Long userId) {
        if (userId == null) return null;
        return nameCache.computeIfAbsent(userId, id -> {
            User u = userMapper.selectById(id);
            return u != null ? u.getRealName() : null;
        });
    }

    private WorkOrderResponse toResponse(WorkOrder wo) {
        nameCache.clear();
        WorkOrderResponse r = new WorkOrderResponse();
        r.setId(wo.getId());
        r.setTenantId(wo.getTenantId());
        r.setRegionId(wo.getRegionId());
        r.setType(wo.getType());
        r.setTitle(wo.getTitle());
        r.setDescription(wo.getDescription());
        r.setAssetId(wo.getAssetId());
        r.setReporterId(wo.getReporterId());
        r.setReporterName(getUserName(wo.getReporterId()));
        r.setEngineerId(wo.getEngineerId());
        r.setEngineerName(getUserName(wo.getEngineerId()));
        r.setStatus(wo.getStatus());
        r.setPriority(wo.getPriority());
        r.setResolution(wo.getResolution());
        r.setRating(wo.getRating());
        r.setCreatedAt(wo.getCreatedAt());
        r.setUpdatedAt(wo.getUpdatedAt());
        return r;
    }
}
