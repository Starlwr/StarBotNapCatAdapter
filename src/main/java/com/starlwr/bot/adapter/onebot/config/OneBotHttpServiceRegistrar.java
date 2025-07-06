package com.starlwr.bot.adapter.onebot.config;

import com.starlwr.bot.adapter.onebot.proxy.OneBotHttpServiceProxy;
import com.starlwr.bot.adapter.onebot.service.OneBotHttpService;
import com.starlwr.bot.core.plugin.StarBotComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Proxy;

/**
 * OneBot HTTP 服务注册器
 */
@Slf4j
@Configuration
@StarBotComponent
public class OneBotHttpServiceRegistrar {
    @Bean
    public OneBotHttpServiceProxy oneBotHttpServiceProxy() {
        return new OneBotHttpServiceProxy();
    }

    @Bean
    public OneBotHttpService oneBotHttpService(OneBotHttpServiceProxy proxy) {
        return (OneBotHttpService) Proxy.newProxyInstance(
                OneBotHttpService.class.getClassLoader(),
                new Class[]{OneBotHttpService.class},
                proxy
        );
    }
}
