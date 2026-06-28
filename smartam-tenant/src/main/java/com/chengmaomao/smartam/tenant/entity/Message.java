package com.chengmaomao.smartam.tenant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long recipientId;
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
