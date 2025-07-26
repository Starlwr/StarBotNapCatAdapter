package com.starlwr.bot.adapter.onebot.service;

import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.adapter.onebot.converter.OneBotMessageConverter;
import com.starlwr.bot.adapter.onebot.enums.ResultCode;
import com.starlwr.bot.adapter.onebot.exception.OneBotApiException;
import com.starlwr.bot.adapter.onebot.http.OneBotHttpAdapter;
import com.starlwr.bot.adapter.onebot.model.OneBotSender;
import com.starlwr.bot.core.enums.PushTargetType;
import com.starlwr.bot.core.model.Message;
import com.starlwr.bot.core.plugin.StarBotComponent;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

/**
 * OneBot HTTP 服务
 */
@Slf4j
@Order(-10000)
@StarBotComponent
public class OneBotHttpService implements ApplicationListener<ApplicationReadyEvent> {
    @Resource
    private OneBotHttpAdapter http;

    @Resource
    private OneBotMessageConverter converter;

    private final Map<String, OneBotSender> senders = new HashMap<>();

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        for (String senderName : senders.keySet()) {
            OneBotSender sender = senders.get(senderName);

            log.info("开始检测 {} 的 OneBot HTTP 服务可用性", senderName);
            log.info("OneBot HTTP 连接地址: http://{}:{}", sender.getOneBotAddress(), sender.getOneBotPort());
            try {
                JSONObject versionInfo = http.getVersionInfo(sender, new JSONObject());
                log.info("OneBot HTTP 连接正常, 版本 v{}", versionInfo.getString("app_version"));
            } catch (WebClientResponseException.Forbidden e) {
                log.error("OneBot HTTP Token 配置不正确, 请检查", e);
            } catch (Exception e) {
                log.error("OneBot HTTP 服务不可用, 请检查配置和服务状态", e);
            }
        }
    }

    /**
     * 注册 OneBot HTTP 推送平台
     * @param sender OneBot HTTP 推送平台
     */
    public void registerSender(OneBotSender sender) {
        senders.put(sender.getName(), sender);
    }

    /**
     * 发送消息到 OneBot
     *
     * @param message 消息
     */
    public JSONObject send(Message message) {
        OneBotSender sender = senders.get(message.getPlatform());

        try {
            JSONObject params = new JSONObject();
            params.put("message", converter.convert(message.getContent()));

            if (message.getType() == PushTargetType.FRIEND) {
                params.put("user_id", String.valueOf(message.getNum()));
                http.sendPrivateMsg(sender, params);
            } else if (message.getType() == PushTargetType.GROUP) {
                params.put("group_id", String.valueOf(message.getNum()));
                http.sendGroupMsg(sender, params);
            } else {
                return new JSONObject().fluentPut("code", ResultCode.UNKNOWN_TARGET_TYPE.getCode()).fluentPut("message", ResultCode.UNKNOWN_TARGET_TYPE.getMsg());
            }

            return new JSONObject().fluentPut("code", ResultCode.SUCCESS.getCode()).fluentPut("message", ResultCode.SUCCESS.getMsg());
        } catch (OneBotApiException e) {
            return new JSONObject().fluentPut("code", ResultCode.API_ERROR.getCode()).fluentPut("message", ResultCode.API_ERROR.getMsg() + ": " + e.getMsg());
        } catch (Exception e) {
            log.error("OneBot HTTP 发送消息异常", e);
            return new JSONObject().fluentPut("code", ResultCode.UNKNOWN.getCode()).fluentPut("message", "OneBot HTTP 发送消息异常, 请检查插件日志错误信息");
        }
    }
}
