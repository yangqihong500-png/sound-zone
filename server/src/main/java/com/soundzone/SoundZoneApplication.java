package com.soundzone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 同频 SoundZone 后端启动类 产品定义见 ../docs/01~04，开发方案见 ../docs/05 */
@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class SoundZoneApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoundZoneApplication.class, args);
    }
}
