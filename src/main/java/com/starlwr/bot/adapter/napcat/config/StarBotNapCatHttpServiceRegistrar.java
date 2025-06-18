package com.starlwr.bot.adapter.napcat.config;

import com.starlwr.bot.adapter.napcat.proxy.StarBotNapCatHttpServiceProxy;
import com.starlwr.bot.adapter.napcat.service.StarBotNapCatHttpService;
import com.starlwr.bot.core.plugin.StarBotComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Proxy;

/**
 * StarBot NapCat HTTP 服务注册器
 */
@Slf4j
@Configuration
@StarBotComponent
public class StarBotNapCatHttpServiceRegistrar {
    @Bean
    public StarBotNapCatHttpServiceProxy starBotNapCatHttpServiceProxy() {
        return new StarBotNapCatHttpServiceProxy();
    }

    @Bean
    public StarBotNapCatHttpService starBotNapCatHttpService(StarBotNapCatHttpServiceProxy proxy) {
        return (StarBotNapCatHttpService) Proxy.newProxyInstance(
                StarBotNapCatHttpService.class.getClassLoader(),
                new Class[]{StarBotNapCatHttpService.class},
                proxy
        );
    }
}
