package com.starlwr.bot.adapter.onebot.config;

import com.starlwr.bot.adapter.onebot.model.OneBotSender;
import com.starlwr.bot.core.plugin.StarBotComponent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * StarBotOneBotAdapterPlugin 配置类
 */
@Getter
@Setter
@Configuration
@StarBotComponent
@ConfigurationProperties(prefix = "starbot.adapter.onebot")
public class StarBotOneBotAdapterPluginProperties {
    /**
     * OneBot 推送接口统一前缀
     */
    private String baseUrl = "/onebot";

    /**
     * OneBot 推送平台列表
     */
    private List<OneBotSender> senders = new ArrayList<>();
}
