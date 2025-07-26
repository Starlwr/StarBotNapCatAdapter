package com.starlwr.bot.adapter.onebot.model;

import lombok.Getter;
import lombok.Setter;

/**
 * OneBot 推送平台信息
 */
@Getter
@Setter
public class OneBotSender {
    /**
     * 名称
     */
    private String name;

    /**
     * 开放推送接口地址，用于设置多机器人推送，例如：/send
     */
    private String api;

    /**
     * OneBot 地址
     */
    private String oneBotAddress;

    /**
     * OneBot 端口号
     */
    private int oneBotPort;

    /**
     * OneBot Token
     */
    private String oneBotToken = "";

    /**
     * 消息发送间隔时间，单位：毫秒
     */
    private int delay = 0;

    /**
     * 请求信息 Debug 日志最大输出长度，设置为 0 不限制长度
     */
    private int debugLogMaxLength = 1000;
}
