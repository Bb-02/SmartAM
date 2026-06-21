package com.chengmaomao.smartam.tenant.entity;

public final class RoleEnum {
    private RoleEnum() {}

    public static final String EMPLOYEE = "EMPLOYEE";
    public static final String ENGINEER = "ENGINEER";
    public static final String ADMIN_REGION = "ADMIN_REGION";
    public static final String ADMIN_TENANT = "ADMIN_TENANT";

    public static boolean isValid(String role) {
        return EMPLOYEE.equals(role) || ENGINEER.equals(role)
                || ADMIN_REGION.equals(role) || ADMIN_TENANT.equals(role);
    }
}
