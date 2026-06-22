package com.chengmaomao.smartam.tenant.entity;

public final class AssetStatus {
    private AssetStatus() {}

    public static final String IN_STORAGE = "IN_STORAGE";
    public static final String IN_USE = "IN_USE";
    public static final String IN_REPAIR = "IN_REPAIR";
    public static final String SCRAPPED = "SCRAPPED";

    public static boolean isValid(String status) {
        return IN_STORAGE.equals(status) || IN_USE.equals(status)
                || IN_REPAIR.equals(status) || SCRAPPED.equals(status);
    }
}
