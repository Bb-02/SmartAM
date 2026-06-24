package com.chengmaomao.smartam.tenant.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.chengmaomao.smartam.tenant.config.handler.MyMetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MyMetaObjectHandler myMetaObjectHandler() {
        log.info("注册 MetaObjectHandler（时间戳自动填充）");
        return new MyMetaObjectHandler();
    }

    @Bean
    public GlobalConfig globalConfig(MyMetaObjectHandler handler) {
        log.info("注册 GlobalConfig + MetaObjectHandler");
        GlobalConfig gc = new GlobalConfig();
        gc.setMetaObjectHandler(handler);
        return gc;
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("注册 MybatisPlus 分页插件");
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
