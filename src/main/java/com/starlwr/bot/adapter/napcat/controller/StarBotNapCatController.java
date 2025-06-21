package com.starlwr.bot.adapter.napcat.controller;

import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.adapter.napcat.config.StarBotNapCatAdapterProperties;
import com.starlwr.bot.adapter.napcat.converter.NapCatMessageConverter;
import com.starlwr.bot.adapter.napcat.enums.ResultCode;
import com.starlwr.bot.adapter.napcat.exception.NapCatApiException;
import com.starlwr.bot.adapter.napcat.service.StarBotNapCatHttpService;
import com.starlwr.bot.core.enums.PushTargetType;
import com.starlwr.bot.core.model.Message;
import com.starlwr.bot.core.plugin.StarBotComponent;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Resource
    private NapCatMessageConverter converter;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        log.info("NapCat 连接地址: http://{}:{}", properties.getAddress(), properties.getPort());
        log.info("开始检测 NapCat 服务可用性");
        JSONObject versionInfo = napcat.getVersionInfo(new JSONObject());
        log.info("NapCat 连接正常, 版本 v{}", versionInfo.getString("app_version"));
    }

    @PostMapping("/send")
    public JSONObject send(@RequestBody Message message) {
        try {
            JSONObject params = new JSONObject();
            params.put("message", converter.convert(message.getContent()));

            if (message.getType() == PushTargetType.FRIEND) {
                params.put("user_id", String.valueOf(message.getNum()));
                napcat.sendPrivateMsg(params);
            } else if (message.getType() == PushTargetType.GROUP) {
                params.put("group_id", String.valueOf(message.getNum()));
                napcat.sendGroupMsg(params);
            } else {
                return new JSONObject().fluentPut("code", ResultCode.UNKNOWN_TARGET_TYPE.getCode()).fluentPut("message", ResultCode.UNKNOWN_TARGET_TYPE.getMsg());
            }

            return new JSONObject().fluentPut("code", ResultCode.SUCCESS.getCode()).fluentPut("message", ResultCode.SUCCESS.getMsg());
        } catch (NapCatApiException e) {
            return new JSONObject().fluentPut("code", ResultCode.API_ERROR.getCode()).fluentPut("message", ResultCode.API_ERROR.getMsg() + ": " + e);
        } catch (Exception e) {
            log.error("NapCat 发送消息异常", e);
            return new JSONObject().fluentPut("code", ResultCode.UNKNOWN.getCode()).fluentPut("message", "NapCat 发送消息异常, 请检查插件日志错误信息");
        }
    }
}
