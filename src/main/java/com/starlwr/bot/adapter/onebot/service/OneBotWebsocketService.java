package com.starlwr.bot.adapter.onebot.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.adapter.onebot.model.OneBotSender;
import com.starlwr.bot.core.plugin.StarBotComponent;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * OneBot Websocket 服务
 */
@Slf4j
@Order(-10000)
@StarBotComponent
public class OneBotWebsocketService implements ApplicationListener<ApplicationReadyEvent> {
    @Resource
    @Qualifier("oneBotThreadPool")
    private ThreadPoolTaskExecutor executor;

    private final Map<String, OneBotSender> senders = new HashMap<>();

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        for (OneBotSender sender : senders.values()) {
            connect(sender);
        }
    }

    /**
     * 注册 OneBot HTTP 推送平台
     * @param sender OneBot HTTP 推送平台
     */
    public void register(OneBotSender sender) {
        senders.put(sender.getName(), sender);
    }

    /**
     * 连接到 OneBot Websocket 服务
     * @param sender OneBot 推送平台信息
     */
    public void connect(OneBotSender sender) {
        executor.submit(() -> {
            int retryCount = 0;
            int retryInterval = 1;
            while (true) {
                log.info("准备连接 {} 的 OneBot Websocket 服务", sender.getName());
                log.info("OneBot Websocket 连接地址: ws://{}:{}/", sender.getOneBotAddress(), sender.getOneBotWebsocketPort());

                try {
                    String url = String.format("ws://%s:%d", sender.getOneBotAddress(), sender.getOneBotWebsocketPort());

                    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
                    headers.add("Authorization", "Bearer " + sender.getOneBotToken());

                    StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
                    OneBotWebSocketHandler handler = new OneBotWebSocketHandler(this, sender);
                    CompletableFuture<WebSocketSession> sessionFuture = webSocketClient.execute(handler, headers, URI.create(url));

                    sessionFuture.get(3, TimeUnit.SECONDS);

                    break;
                } catch (Exception e) {
                    retryCount++;
                    retryInterval = Math.min(retryInterval * 2, 60);

                    if (e instanceof TimeoutException) {
                        log.warn("连接 {} 的 OneBot Websocket 服务超时, 将在 {} 秒后进行第 {} 次重试", sender.getName(), retryInterval, retryCount);
                    } else {
                        log.error("{} 的 OneBot Websocket 服务不可用, 请检查配置和服务状态, 将在 {} 秒后进行第 {} 次重试", sender.getName(), retryInterval, retryCount, e);
                    }

                    try {
                        Thread.sleep(retryInterval * 1000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.error("连接 {} 的 OneBot Websocket 中断", sender.getName(), ex);
                    }
                }
            }
        });
    }

    /**
     * WebSocket 处理器
     */
    private static class OneBotWebSocketHandler implements WebSocketHandler {
        private final OneBotWebsocketService service;

        private final OneBotSender sender;

        private final ThreadPoolTaskExecutor executor;

        private OneBotWebSocketHandler(OneBotWebsocketService service, OneBotSender sender) {
            this.service = service;
            this.sender = sender;
            this.executor = service.executor;
        }

        /**
         * 连接建立
         * @param session WebSocket 会话
         */
        @Override
        public void afterConnectionEstablished(@NonNull WebSocketSession session) {
            executor.submit(() -> log.info("已连接到 {} 的 OneBot Websocket 服务", sender.getName()));
        }

        /**
         * 消息处理
         * @param session WebSocket 会话
         * @param webSocketRawMessage WebSocket 消息
         */
        @Override
        public void handleMessage(@NonNull WebSocketSession session, @NonNull WebSocketMessage<?> webSocketRawMessage) {
            executor.submit(() -> {
                try {
                    if (webSocketRawMessage instanceof TextMessage webSocketMessage) {
                        JSONObject rawMessage = JSON.parseObject(webSocketMessage.getPayload());
                        if ("status".equalsIgnoreCase(rawMessage.getString("raw_message"))) {
                            JSONObject operation = new JSONObject();
                            operation.put("reply", "Running on StarBot v3.0.0");

                            JSONObject params = new JSONObject();
                            params.put("context", rawMessage);
                            params.put("operation", operation);

                            JSONObject response = new JSONObject();
                            response.put("action", ".handle_quick_operation");
                            response.put("params", params);

                            session.sendMessage(new TextMessage(response.toJSONString()));
                        }
                    }
                } catch (Exception e) {
                    log.error("处理 {} 的 OneBot Websocket 消息时发生异常", sender.getName(), e);
                }
            });
        }

        /**
         * 传输错误
         * @param session WebSocket 会话
         * @param exception 异常
         */
        @Override
        public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
            executor.submit(() -> {
                log.warn("与 {} 的 Websocket 连接异常, 将在 1 秒后重新连接", sender.getName(), exception);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("重新连接 {} 的 Websocket 时中断", sender.getName(), e);
                }
                service.connect(sender);
            });
        }

        /**
         * 连接关闭
         * @param session WebSocket 会话
         * @param closeStatus 关闭状态
         */
        @Override
        public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus closeStatus) {
            executor.submit(() -> {
                log.warn("与 {} 的 Websocket 连接断开, 将在 1 秒后重新连接", sender.getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("重新连接 {} 的 Websocket 时中断", sender.getName(), e);
                }
                service.connect(sender);
            });
        }

        /**
         * 是否支持部分消息
         * @return 是否支持部分消息
         */
        @Override
        public boolean supportsPartialMessages() {
            return false;
        }
    }
}
