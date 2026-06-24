package com.chengmaomao.smartam.tenant.config.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    @PostConstruct
    public void init() {
        log.info("MyMetaObjectHandler 已注册");
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("insertFill 被调用，实体: {}", metaObject.getOriginalObject().getClass().getSimpleName());
        LocalDateTime now = LocalDateTime.now();
        this.setFieldValByName("createdAt", now, metaObject);
        this.setFieldValByName("updatedAt", now, metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("updateFill 被调用，实体: {}", metaObject.getOriginalObject().getClass().getSimpleName());
        this.setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
    }
}
