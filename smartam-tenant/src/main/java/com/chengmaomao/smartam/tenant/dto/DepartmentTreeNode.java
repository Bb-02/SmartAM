package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DepartmentTreeNode {
    private Long id;
    private String name;
    private String code;
    private Long parentId;
    private Long regionId;
    private List<DepartmentTreeNode> children = new ArrayList<>();
}
