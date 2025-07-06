package com.starlwr.bot.adapter.onebot.controller;

import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.adapter.onebot.config.StarBotOneBotAdapterPluginProperties;
import com.starlwr.bot.adapter.onebot.converter.OneBotMessageConverter;
import com.starlwr.bot.adapter.onebot.enums.ResultCode;
import com.starlwr.bot.adapter.onebot.exception.OneBotApiException;
import com.starlwr.bot.adapter.onebot.service.OneBotHttpService;
import com.starlwr.bot.core.enums.PushTargetType;
import com.starlwr.bot.core.model.Message;
import com.starlwr.bot.core.plugin.StarBotComponent;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * OneBot 控制器
 */
@Slf4j
@Order(-10000)
@RestController
@StarBotComponent
@RequestMapping("/onebot")
public class OneBotController implements ApplicationListener<ApplicationReadyEvent> {
    @Resource
    private StarBotOneBotAdapterPluginProperties properties;

    @Resource
    private OneBotHttpService onebot;

    @Resource
    private OneBotMessageConverter converter;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        log.info("OneBot 连接地址: http://{}:{}", properties.getAddress(), properties.getPort());
        log.info("开始检测 OneBot 服务可用性");
        try {
            JSONObject versionInfo = onebot.getVersionInfo(new JSONObject());
            log.info("OneBot 连接正常, 版本 v{}", versionInfo.getString("app_version"));
        } catch (WebClientResponseException.Forbidden e) {
            log.error("OneBot Token 配置不正确, 请检查", e);
        } catch (Exception e) {
            log.error("OneBot 服务不可用, 请检查配置和服务状态", e);
        }
    }

    @PostMapping("/send")
    public JSONObject send(@RequestBody Message message) {
        try {
            JSONObject params = new JSONObject();
            params.put("message", converter.convert(message.getContent()));

            if (message.getType() == PushTargetType.FRIEND) {
                params.put("user_id", String.valueOf(message.getNum()));
                onebot.sendPrivateMsg(params);
            } else if (message.getType() == PushTargetType.GROUP) {
                params.put("group_id", String.valueOf(message.getNum()));
                onebot.sendGroupMsg(params);
            } else {
                return new JSONObject().fluentPut("code", ResultCode.UNKNOWN_TARGET_TYPE.getCode()).fluentPut("message", ResultCode.UNKNOWN_TARGET_TYPE.getMsg());
            }

            return new JSONObject().fluentPut("code", ResultCode.SUCCESS.getCode()).fluentPut("message", ResultCode.SUCCESS.getMsg());
        } catch (OneBotApiException e) {
            return new JSONObject().fluentPut("code", ResultCode.API_ERROR.getCode()).fluentPut("message", ResultCode.API_ERROR.getMsg() + ": " + e.getMsg());
        } catch (Exception e) {
            log.error("OneBot 发送消息异常", e);
            return new JSONObject().fluentPut("code", ResultCode.UNKNOWN.getCode()).fluentPut("message", "OneBot 发送消息异常, 请检查插件日志错误信息");
        }
    }
}
