package com.soundzone.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** 业务时钟统一使用上海时区，与历史 DATETIME 数据一致；协议输出毫秒时间戳。 */
@Configuration
public class BusinessConfig {
    @Bean
    public Clock businessClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }
}
