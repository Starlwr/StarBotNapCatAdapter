package com.starlwr.bot.adapter.onebot.config;

import com.starlwr.bot.adapter.onebot.http.OneBotHttpAdapterProxy;
import com.starlwr.bot.adapter.onebot.http.OneBotHttpAdapter;
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
public class OneBotHttpAdapterRegistrar {
    @Bean
    public OneBotHttpAdapterProxy oneBotHttpAdapterProxy() {
        return new OneBotHttpAdapterProxy();
    }

    @Bean
    public OneBotHttpAdapter oneBotHttpAdapter(OneBotHttpAdapterProxy proxy) {
        return (OneBotHttpAdapter) Proxy.newProxyInstance(
                OneBotHttpAdapter.class.getClassLoader(),
                new Class[]{OneBotHttpAdapter.class},
                proxy
        );
    }
}
