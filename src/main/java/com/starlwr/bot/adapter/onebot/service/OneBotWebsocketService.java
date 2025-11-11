package com.starlwr.bot.adapter.onebot.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.adapter.onebot.config.OneBotAdapterPluginProperties;
import com.starlwr.bot.adapter.onebot.model.OneBotSender;
import com.starlwr.bot.core.plugin.StarBotComponent;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * OneBot Websocket 服务
 */
@Slf4j
@StarBotComponent
public class OneBotWebsocketService {
    private final ThreadPoolTaskExecutor executor;

    private final OneBotAdapterPluginProperties properties;

    @Autowired
    public OneBotWebsocketService(@Qualifier("oneBotThreadPool") ThreadPoolTaskExecutor executor, OneBotAdapterPluginProperties properties) {
        this.executor = executor;
        this.properties = properties;
    }

    /**
     * 连接 OneBot Websocket
     */
    @Order(-10000)
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReadyEvent() {
        for (OneBotSender sender : properties.getSenders()) {
            if (sender.isWebsocket()) {
                connect(sender);
            }
        }
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

                CompletableFuture<WebSocketSession> sessionFuture = null;
                try {
                    String url = String.format("ws://%s:%d", sender.getOneBotAddress(), sender.getOneBotWebsocketPort());

                    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
                    headers.add("Authorization", "Bearer " + sender.getOneBotWebsocketToken());

                    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                    container.setDefaultMaxTextMessageBufferSize(8 * 1024 * 1024);
                    StandardWebSocketClient webSocketClient = new StandardWebSocketClient(container);
                    OneBotWebSocketHandler handler = new OneBotWebSocketHandler(this, sender);
                    sessionFuture = webSocketClient.execute(handler, headers, URI.create(url));

                    if (handler.awaitConnection()) {
                        sessionFuture.get();
                        break;
                    } else {
                        throw new TimeoutException();
                    }
                } catch (Exception e) {
                    retryCount++;
                    retryInterval = Math.min(retryInterval * 2, 60);

                    if (e instanceof TimeoutException) {
                        log.warn("连接 {} 的 OneBot Websocket 服务超时, 将在 {} 秒后进行第 {} 次重试", sender.getName(), retryInterval, retryCount);
                        sessionFuture.cancel(true);
                    } else {
                        log.error("{} 的 OneBot Websocket 服务不可用, 请检查配置和服务状态, 将在 {} 秒后进行第 {} 次重试", sender.getName(), retryInterval, retryCount, e);
                    }

                    try {
                        Thread.sleep(retryInterval * 1000L);
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

        private final CountDownLatch latch = new CountDownLatch(1);

        private final StringBuilder messageBuffer = new StringBuilder();

        private boolean connectTimeout = false;

        private OneBotWebSocketHandler(OneBotWebsocketService service, OneBotSender sender) {
            this.service = service;
            this.sender = sender;
            this.executor = service.executor;
        }

        /**
         * 等待 WebSocket 连接成功
         * @return 连接是否成功
         */
        public boolean awaitConnection() {
            synchronized (this) {
                try {
                    if (latch.await(3, TimeUnit.SECONDS)) {
                        return true;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                connectTimeout = true;
                return false;
            }
        }

        /**
         * 连接建立
         * @param session WebSocket 会话
         */
        @Override
        public void afterConnectionEstablished(@NonNull WebSocketSession session) {
            latch.countDown();

            synchronized (this) {
                if (connectTimeout) {
                    try {
                        session.close();
                    } catch (Exception e) {
                        log.error("断开 {} 的超时 OneBot Websocket 服务异常", sender.getName(), e);
                    }
                    return;
                }
            }

            executor.submit(() -> log.info("已连接到 {} 的 OneBot Websocket 服务", sender.getName()));
        }

        /**
         * 消息处理
         * @param session WebSocket 会话
         * @param webSocketRawMessage WebSocket 消息
         */
        @Override
        public void handleMessage(@NonNull WebSocketSession session, @NonNull WebSocketMessage<?> webSocketRawMessage) {
            try {
                if (webSocketRawMessage instanceof TextMessage webSocketMessage) {
                    messageBuffer.append(webSocketMessage.getPayload());

                    if (webSocketMessage.isLast()) {
                        String fullMessage = messageBuffer.toString();
                        messageBuffer.setLength(0);

                        executor.submit(() -> {
                            try {
                                JSONObject rawMessage = JSON.parseObject(fullMessage);
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
                            } catch (Exception e) {
                                log.error("处理 {} 的 OneBot Websocket 消息时发生异常", sender.getName(), e);
                                messageBuffer.setLength(0);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                log.error("处理 {} 的 OneBot Websocket 分片消息发生异常", sender.getName(), e);
                messageBuffer.setLength(0);
            }
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
            if (connectTimeout) {
                return;
            }

            executor.submit(() -> {
                log.warn("与 {} 的 Websocket 连接断开 ({}: {}), 将在 1 秒后重新连接", sender.getName(), closeStatus.getCode(), closeStatus.getReason());
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
            return true;
        }
    }
}
