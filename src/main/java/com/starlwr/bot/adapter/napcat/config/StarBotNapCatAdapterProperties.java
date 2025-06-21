package com.starlwr.bot.adapter.napcat.config;

import com.starlwr.bot.core.plugin.StarBotComponent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * StarBotNapCatAdapter 配置类
 */
@Getter
@Setter
@Configuration
@StarBotComponent
@ConfigurationProperties(prefix = "starbot.adapter.napcat")
public class StarBotNapCatAdapterProperties {
    /**
     * 接口地址
     */
    private String address = "localhost";

    /**
     * 端口号
     */
    private int port = 3000;

    /**
     * Token
     */
    private String token;

    /**
     * 请求信息 Debug 日志最大输出长度
     */
    private int debugLogMaxLength = 1000;
}
