package com.chengmaomao.smartam.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chengmaomao.smartam.common.exception.BusinessException;
import com.chengmaomao.smartam.common.security.JwtUser;
import com.chengmaomao.smartam.tenant.dto.MessageResponse;
import com.chengmaomao.smartam.tenant.entity.Message;
import com.chengmaomao.smartam.tenant.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageMapper messageMapper;

    private JwtUser currentUser() {
        return (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public void send(Long tenantId, Long recipientId, String type,
                     String title, String content, Long relatedId) {
        try {
            Message msg = new Message();
            msg.setTenantId(tenantId);
            msg.setRecipientId(recipientId);
            msg.setType(type);
            msg.setTitle(title);
            msg.setContent(content);
            msg.setRelatedId(relatedId);
            msg.setIsRead(0);
            messageMapper.insert(msg);
        } catch (Exception e) {
            log.warn("发送消息失败 recipientId={} type={} title={}", recipientId, type, title, e);
        }
    }

    public IPage<MessageResponse> page(int page, int size, String type, Integer isRead) {
        JwtUser me = currentUser();
        LambdaQueryWrapper<Message> qw = new LambdaQueryWrapper<>();
        qw.eq(Message::getRecipientId, me.getUserId());
        if (type != null && !type.isBlank()) {
            qw.eq(Message::getType, type);
        }
        if (isRead != null) {
            qw.eq(Message::getIsRead, isRead);
        }
        qw.orderByDesc(Message::getCreatedAt);
        Page<Message> result = messageMapper.selectPage(Page.of(page, size), qw);
        return result.convert(this::toResponse);
    }

    public Long unreadCount() {
        JwtUser me = currentUser();
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getRecipientId, me.getUserId())
                .eq(Message::getIsRead, 0));
    }

    public void markRead(Long id) {
        JwtUser me = currentUser();
        Message msg = messageMapper.selectById(id);
        if (msg == null || !msg.getRecipientId().equals(me.getUserId())) {
            throw new BusinessException("消息不存在");
        }
        msg.setIsRead(1);
        messageMapper.updateById(msg);
    }

    public void markAllRead() {
        JwtUser me = currentUser();
        LambdaUpdateWrapper<Message> uw = new LambdaUpdateWrapper<>();
        uw.eq(Message::getRecipientId, me.getUserId())
          .eq(Message::getIsRead, 0)
          .set(Message::getIsRead, 1);
        messageMapper.update(null, uw);
    }

    private MessageResponse toResponse(Message msg) {
        MessageResponse r = new MessageResponse();
        r.setId(msg.getId());
        r.setType(msg.getType());
        r.setTitle(msg.getTitle());
        r.setContent(msg.getContent());
        r.setRelatedId(msg.getRelatedId());
        r.setIsRead(msg.getIsRead());
        r.setCreatedAt(msg.getCreatedAt());
        return r;
    }
}
