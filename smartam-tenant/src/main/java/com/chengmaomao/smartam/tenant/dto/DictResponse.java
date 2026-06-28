package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

import java.util.List;

@Data
public class DictResponse {
    private List<String> assetStatuses;
    private List<String> workOrderStatuses;
    private List<String> workOrderTypes;
    private List<String> workOrderPriorities;
    private List<String> applicationStatuses;
    private List<String> applicationTypes;
    private List<String> roles;
    private List<String> messageTypes;
}
