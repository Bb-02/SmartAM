package com.chengmaomao.smartam.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssetApplicationCreateRequest {
    @NotNull(message = "资产ID不能为空")
    private Long assetId;

    @NotBlank(message = "申请原因不能为空")
    private String reason;

    private String type;

    /** TRANSFER 专用：目标领用人 */
    private Long targetUserId;

    /** TRANSFER 专用：目标部门 */
    private Long targetDeptId;
}
