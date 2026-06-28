package com.chengmaomao.smartam.tenant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chengmaomao.smartam.common.result.ApiResponse;
import com.chengmaomao.smartam.tenant.dto.MessageResponse;
import com.chengmaomao.smartam.tenant.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ApiResponse<IPage<MessageResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(messageService.page(page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.ok(messageService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        messageService.markRead(id);
        return ApiResponse.ok();
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        messageService.markAllRead();
        return ApiResponse.ok();
    }
}
