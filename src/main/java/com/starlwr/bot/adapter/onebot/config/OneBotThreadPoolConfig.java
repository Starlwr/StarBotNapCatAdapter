package com.starlwr.bot.adapter.onebot.config;

import com.starlwr.bot.core.plugin.StarBotComponent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * StarBotOneBotAdapterPlugin 线程池配置类
 */
@Slf4j
@Configuration
@StarBotComponent
public class OneBotThreadPoolConfig {
    @Resource
    private OneBotAdapterPluginProperties properties;

    @Bean
    public ThreadPoolTaskExecutor oneBotThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getWebsocketThread().getCorePoolSize());
        executor.setMaxPoolSize(properties.getWebsocketThread().getMaxPoolSize());
        executor.setQueueCapacity(properties.getWebsocketThread().getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getWebsocketThread().getKeepAliveSeconds());
        executor.setThreadNamePrefix("onebot-thread-");
        executor.setRejectedExecutionHandler(new OneBotWithLogCallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    private static class OneBotWithLogCallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("OneBot 线程池资源已耗尽, 请考虑增加线程池大小!");
            r.run();
        }
    }
}
