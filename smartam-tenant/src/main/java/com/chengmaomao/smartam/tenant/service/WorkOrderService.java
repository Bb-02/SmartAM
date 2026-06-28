package com.chengmaomao.smartam.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chengmaomao.smartam.common.exception.BusinessException;
import com.chengmaomao.smartam.common.security.JwtUser;
import com.chengmaomao.smartam.tenant.dto.WorkOrderConfirmRequest;
import com.chengmaomao.smartam.tenant.dto.WorkOrderCreateRequest;
import com.chengmaomao.smartam.tenant.dto.WorkOrderResolveRequest;
import com.chengmaomao.smartam.tenant.dto.WorkOrderUpdateRequest;
import com.chengmaomao.smartam.tenant.dto.WorkOrderResponse;
import com.chengmaomao.smartam.tenant.entity.Asset;
import com.chengmaomao.smartam.tenant.entity.RoleEnum;
import com.chengmaomao.smartam.tenant.entity.User;
import com.chengmaomao.smartam.tenant.entity.WorkOrder;
import com.chengmaomao.smartam.tenant.entity.WorkOrderLog;
import com.chengmaomao.smartam.tenant.entity.WorkOrderStatus;
import com.chengmaomao.smartam.tenant.mapper.AssetMapper;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderLogMapper workOrderLogMapper;
    private final UserMapper userMapper;
    private final AssetMapper assetMapper;
    private final MessageService messageService;

    private static final Set<String> VALID_PRIORITIES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    private static final Set<String> VALID_TYPES = Set.of("REPAIR");

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

        String type = req.getType() != null ? req.getType() : "REPAIR";
        if (!VALID_TYPES.contains(type)) {
            throw new BusinessException("无效的工单类型: " + type);
        }
        String priority = req.getPriority() != null ? req.getPriority() : "NORMAL";
        if (!VALID_PRIORITIES.contains(priority)) {
            throw new BusinessException("无效的优先级: " + priority);
        }

        if (req.getAssetId() != null) {
            Asset asset = assetMapper.selectById(req.getAssetId());
            if (asset == null || !asset.getTenantId().equals(me.getTenantId())) {
                throw new BusinessException("资产不存在");
            }
            Long dup = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                    .eq(WorkOrder::getAssetId, req.getAssetId())
                    .eq(WorkOrder::getReporterId, me.getUserId())
                    .notIn(WorkOrder::getStatus, WorkOrderStatus.CLOSED, WorkOrderStatus.CANCELLED));
            if (dup > 0) {
                throw new BusinessException("您已对该资产提交过工单，请等待处理或取消后再提交");
            }
        }

        WorkOrder wo = new WorkOrder();
        wo.setTenantId(me.getTenantId());
        wo.setRegionId(me.getRegionId());
        wo.setType(type);
        wo.setTitle(req.getTitle());
        wo.setDescription(req.getDescription());
        wo.setAssetId(req.getAssetId());
        wo.setReporterId(me.getUserId());
        wo.setPriority(priority);
        wo.setStatus(WorkOrderStatus.PENDING);
        workOrderMapper.insert(wo);
        wo = workOrderMapper.selectById(wo.getId());

        writeLog(wo.getId(), null, WorkOrderStatus.PENDING, me.getUserId(), null);

        // 通知同分区所有工程师
        List<User> engineers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, me.getTenantId())
                .eq(User::getRegionId, me.getRegionId())
                .eq(User::getRole, RoleEnum.ENGINEER));
        String reporterName = getUserName(me.getUserId());
        for (User engineer : engineers) {
            messageService.send(me.getTenantId(), engineer.getId(), "WORK_ORDER",
                    "新的报修工单",
                    reporterName + " 提交了报修工单「" + wo.getTitle() + "」，等待接单",
                    wo.getId());
        }

        return toResponse(wo);
    }

    public WorkOrderResponse getById(Long id) {
        return toResponse(getOwnedWorkOrder(id));
    }

    @Transactional
    public WorkOrderResponse update(Long id, WorkOrderUpdateRequest req) {
        JwtUser me = currentUser();
        WorkOrder wo = getOwnedWorkOrder(id);

        if (!me.getUserId().equals(wo.getReporterId())) {
            throw new BusinessException("仅提交人可编辑工单");
        }
        if (!WorkOrderStatus.PENDING.equals(wo.getStatus())) {
            throw new BusinessException("仅待处理状态的工单可编辑");
        }

        if (req.getTitle() != null) wo.setTitle(req.getTitle());
        if (req.getDescription() != null) wo.setDescription(req.getDescription());
        if (req.getPriority() != null) {
            if (!VALID_PRIORITIES.contains(req.getPriority())) {
                throw new BusinessException("无效的优先级: " + req.getPriority());
            }
            wo.setPriority(req.getPriority());
        }
        workOrderMapper.updateById(wo);

        writeLog(wo.getId(), WorkOrderStatus.PENDING, WorkOrderStatus.PENDING, me.getUserId(), "编辑工单");
        return toResponse(wo);
    }

    public IPage<WorkOrderResponse> page(int page, int size, String status, String priority,
                                         String keyword, Long assetId, Long engineerId, Long reporterId) {
        JwtUser me = currentUser();
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
        applyRoleFilter(qw, me);

        if (status != null && !status.isBlank()) {
            qw.eq(WorkOrder::getStatus, status);
        }
        if (priority != null && !priority.isBlank()) {
            qw.eq(WorkOrder::getPriority, priority);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(WorkOrder::getTitle, keyword));
        }
        if (assetId != null) {
            qw.eq(WorkOrder::getAssetId, assetId);
        }
        if (engineerId != null) {
            qw.eq(WorkOrder::getEngineerId, engineerId);
        }
        if (reporterId != null) {
            qw.eq(WorkOrder::getReporterId, reporterId);
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

        messageService.send(wo.getTenantId(), wo.getReporterId(), "WORK_ORDER",
                "工单已修复",
                "你的报修工单「" + wo.getTitle() + "」已修复，请确认",
                wo.getId());

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
    public WorkOrderResponse cancel(Long id) {
        JwtUser me = currentUser();
        WorkOrder wo = getOwnedWorkOrder(id);

        if (!WorkOrderStatus.PENDING.equals(wo.getStatus())) {
            throw new BusinessException("仅待处理状态的工单可取消");
        }
        if (!me.getUserId().equals(wo.getReporterId())) {
            throw new BusinessException("仅提交人可取消工单");
        }

        wo.setStatus(WorkOrderStatus.CANCELLED);
        workOrderMapper.updateById(wo);

        writeLog(wo.getId(), WorkOrderStatus.PENDING, WorkOrderStatus.CANCELLED, me.getUserId(), "提交人取消");
        return toResponse(wo);
    }

    @Transactional
    public WorkOrderResponse reject(Long id, String remark) {
        JwtUser me = currentUser();
        WorkOrder wo = getOwnedWorkOrder(id);

        // 管理员驳回 PENDING → CLOSED
        if ((RoleEnum.ADMIN_TENANT.equals(me.getRole()) || RoleEnum.ADMIN_REGION.equals(me.getRole()))
                && WorkOrderStatus.PENDING.equals(wo.getStatus())) {
            wo.setStatus(WorkOrderStatus.CLOSED);
            workOrderMapper.updateById(wo);
            writeLog(wo.getId(), WorkOrderStatus.PENDING, WorkOrderStatus.CLOSED, me.getUserId(), remark);
            return toResponse(wo);
        }

        // 员工驳回 RESOLVED → IN_WORK
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
