package com.chengmaomao.smartam.tenant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageResponse {
    private Long id;
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    private Integer isRead;
    private LocalDateTime createdAt;
}
