package com.starlwr.bot.adapter.onebot.controller;

import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.adapter.onebot.config.OneBotAdapterPluginProperties;
import com.starlwr.bot.adapter.onebot.model.OneBotSender;
import com.starlwr.bot.adapter.onebot.service.OneBotHttpService;
import com.starlwr.bot.adapter.onebot.service.OneBotWebsocketService;
import com.starlwr.bot.core.model.Message;
import com.starlwr.bot.core.model.Sender;
import com.starlwr.bot.core.plugin.StarBotComponent;
import com.starlwr.bot.core.service.StarBotSenderService;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationListener;
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
@Order(-20000)
@RestController
@StarBotComponent
public class OneBotController implements ApplicationListener<ApplicationReadyEvent> {
    @Resource
    private WebServerApplicationContext webContext;

    @Resource
    private RequestMappingHandlerMapping mapping;

    @Resource
    private OneBotAdapterPluginProperties properties;

    @Resource
    private StarBotSenderService senderService;

    @Resource
    private OneBotHttpService httpService;

    @Resource
    private OneBotWebsocketService websocketService;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        Method method;
        try {
            method = getClass().getMethod("send", Message.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("注册推送 API 异常", e);
        }

        for (OneBotSender sender : properties.getSenders()) {
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
            if (sender.isWebsocket()) {
                websocketService.register(sender);
            }
        }
    }

    /**
     * 发送消息到 OneBot
     * @param message 消息
     * @return 调用结果
     */
    public JSONObject send(@RequestBody Message message) {
        return httpService.send(message);
    }

    @Override
    public boolean supportsAsyncExecution() {
        return false;
    }
}
