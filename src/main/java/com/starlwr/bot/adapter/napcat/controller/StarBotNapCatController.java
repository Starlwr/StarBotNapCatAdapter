package com.starlwr.bot.adapter.napcat.controller;

import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.adapter.napcat.config.StarBotNapCatAdapterProperties;
import com.starlwr.bot.adapter.napcat.service.StarBotNapCatHttpService;
import com.starlwr.bot.core.plugin.StarBotComponent;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * StarBot NapCat 控制器
 */
@Slf4j
@RestController
@StarBotComponent
@RequestMapping("/napcat")
public class StarBotNapCatController implements ApplicationListener<ApplicationReadyEvent> {
    @Resource
    private StarBotNapCatAdapterProperties properties;

    @Resource
    private StarBotNapCatHttpService napcat;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        log.info("NapCat 连接地址: http://{}:{}", properties.getAddress(), properties.getPort());
        log.info("开始检测 NapCat 服务可用性");
        JSONObject versionInfo = napcat.getVersionInfo(new JSONObject());
        log.info("NapCat 连接正常, 版本 v{}", versionInfo.getString("app_version"));
    }
}
