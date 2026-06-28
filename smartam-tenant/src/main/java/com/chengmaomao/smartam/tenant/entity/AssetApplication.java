package com.chengmaomao.smartam.tenant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chengmaomao.smartam.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_application")
public class AssetApplication extends BaseEntity {
    private Long tenantId;
    private Long regionId;
    private Long assetId;
    private Long applicantId;
    private String reason;
    private String status;
    private Long approverId;
    private String remark;
}
