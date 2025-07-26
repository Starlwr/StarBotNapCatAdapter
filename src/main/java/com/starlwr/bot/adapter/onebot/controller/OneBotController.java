package com.starlwr.bot.adapter.onebot.controller;

import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.adapter.onebot.config.StarBotOneBotAdapterPluginProperties;
import com.starlwr.bot.adapter.onebot.model.OneBotSender;
import com.starlwr.bot.adapter.onebot.service.OneBotHttpService;
import com.starlwr.bot.core.model.Message;
import com.starlwr.bot.core.model.Sender;
import com.starlwr.bot.core.plugin.StarBotComponent;
import com.starlwr.bot.core.service.StarBotSenderService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.context.WebServerApplicationContext;
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
    @Resource
    private WebServerApplicationContext webContext;

    @Resource
    private RequestMappingHandlerMapping mapping;

    @Resource
    private StarBotOneBotAdapterPluginProperties properties;

    @Resource
    private StarBotSenderService senderService;

    @Resource
    private OneBotHttpService httpService;

    @PostConstruct
    public void init() throws NoSuchMethodException {
        Method method = getClass().getMethod("send", Message.class);

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

            httpService.registerSender(sender);
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
}
