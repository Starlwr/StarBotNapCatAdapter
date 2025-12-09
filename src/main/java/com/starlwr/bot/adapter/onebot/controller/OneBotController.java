package com.starlwr.bot.adapter.onebot.controller;

import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.adapter.onebot.config.OneBotAdapterPluginProperties;
import com.starlwr.bot.adapter.onebot.dto.MessageDTO;
import com.starlwr.bot.adapter.onebot.model.OneBotSender;
import com.starlwr.bot.adapter.onebot.service.OneBotHttpService;
import com.starlwr.bot.core.model.Sender;
import com.starlwr.bot.core.plugin.StarBotComponent;
import com.starlwr.bot.core.service.StarBotSenderService;
import com.starlwr.bot.core.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * OneBot 控制器
 */
@Slf4j
@RestController
@StarBotComponent
public class OneBotController {
    private final WebServerApplicationContext webContext;

    private final RequestMappingHandlerMapping mapping;

    private final OneBotAdapterPluginProperties properties;

    private final StarBotSenderService senderService;

    private final OneBotHttpService httpService;

    @Autowired
    public OneBotController(WebServerApplicationContext webContext, RequestMappingHandlerMapping mapping, OneBotAdapterPluginProperties properties, StarBotSenderService senderService, OneBotHttpService httpService) {
        this.webContext = webContext;
        this.mapping = mapping;
        this.properties = properties;
        this.senderService = senderService;
        this.httpService = httpService;
    }

    /**
     * 注册 OneBot 推送平台接口
     */
    @Order(-20000)
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReadyEvent() {
        Method method;
        try {
            method = getClass().getMethod("send", MessageDTO.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("注册推送 API 异常", e);
        }

        for (OneBotSender sender : properties.getSenders()) {
            if (StringUtil.isBlank(sender.getOneBotHttpToken())) {
                log.error("推送平台 {} 未配置 OneBot HTTP Token, 请完善配置", sender.getName());
                continue;
            }

            try {
                RequestMappingInfo info = RequestMappingInfo
                        .paths(properties.getBaseUrl() + sender.getApi())
                        .methods(RequestMethod.POST)
                        .build();
                mapping.registerMapping(info, this, method);
            } catch (Exception e) {
                log.error("推送平台 {} 注册异常", sender.getName(), e);
            }

            senderService.addSender(new Sender(sender.getName(), "http://localhost:" + webContext.getWebServer().getPort() + properties.getBaseUrl() + sender.getApi(), sender.getDelay()));

            httpService.register(sender);
        }
    }

    /**
     * 发送消息到 OneBot
     * @param message 消息
     * @return 调用结果
     */
    public JSONObject send(@RequestBody MessageDTO message) {
        return httpService.send(message);
    }
}
