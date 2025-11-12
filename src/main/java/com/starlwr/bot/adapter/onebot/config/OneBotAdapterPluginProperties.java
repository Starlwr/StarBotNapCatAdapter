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
public class OneBotAdapterPluginProperties {
    /**
     * OneBot 推送接口统一前缀
     */
    @Getter
    private String baseUrl = "/onebot";

    /**
     * OneBot 推送平台列表
     */
    @Getter
    private List<OneBotSender> senders = new ArrayList<>();

    @Getter
    private WebsocketThread websocketThread = new WebsocketThread();

    @Getter
    private Detect detect = new Detect();

    /**
     * 线程相关
     */
    @Getter
    @Setter
    public static class WebsocketThread {
        /**
         * 线程池核心线程数
         */
        private int corePoolSize = 10;

        /**
         * 线程池最大线程数
         */
        private int maxPoolSize = 100;

        /**
         * 线程池任务队列容量
         */
        private int queueCapacity = 0;

        /**
         * 非核心线程存活时间，单位：秒
         */
        private int keepAliveSeconds = 300;
    }

    /**
     * 检测相关
     */
    @Getter
    @Setter
    public static class Detect {
        /**
         * 是否启用 Websocket 消息接收检测
         */
        private boolean enableWebsocketDetect = false;

        /**
         * 指定时间内未从 Websocket 接收到消息时发送告警邮件，单位: 秒
         */
        private int websocketDetectInterval = 1800;

        /**
         * Websocket 告警邮件发送最短间隔时间，用于防止短时间内发送大量告警邮件，单位: 秒
         */
        private int websocketAlarmMailInterval = 3600;
    }
}
