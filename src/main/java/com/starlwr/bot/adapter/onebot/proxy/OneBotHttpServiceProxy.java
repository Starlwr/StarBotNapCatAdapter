package com.starlwr.bot.adapter.onebot.proxy;

import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.adapter.onebot.annotation.OneBotHttpApi;
import com.starlwr.bot.adapter.onebot.config.StarBotOneBotAdapterPluginProperties;
import com.starlwr.bot.adapter.onebot.exception.OneBotApiException;
import com.starlwr.bot.core.util.HttpUtil;
import com.starlwr.bot.core.util.StringUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * StarBot OneBot HTTP 服务代理
 */
@Slf4j
public class OneBotHttpServiceProxy implements InvocationHandler {
    @Resource
    private StarBotOneBotAdapterPluginProperties properties;

    @Resource
    private HttpUtil http;

    @Getter
    private String apiBaseUrl;

    private final Map<String, String> headers = new HashMap<>();

    @PostConstruct
    public void init() {
        apiBaseUrl = "http://" + properties.getAddress() + ":" + properties.getPort();
        headers.put("Authorization", "Bearer " + properties.getToken());
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() == Object.class) {
            switch (method.getName()) {
                case "toString":
                    return this.toString();
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
            }
        }

        if (method.isAnnotationPresent(OneBotHttpApi.class)) {
            OneBotHttpApi api = method.getAnnotation(OneBotHttpApi.class);
            if (api != null) {
                JSONObject params = (JSONObject) args[0];

                log.debug("OneBotHttpApi <- : {} {}", api.url(), StringUtil.getOmitString(params.toJSONString(), properties.getDebugLogMaxLength()));
                String url = apiBaseUrl + api.url();
                JSONObject result = http.postJson(url, headers, params);
                log.debug("OneBotHttpApi -> : {} {}", api.url(), result.toJSONString());

                if (result.getInteger("retcode") != 0) {
                    throw new OneBotApiException(api.url(), params, result.getInteger("retcode"), result.getString("message"));
                }

                return result.getJSONObject("data");
            }
        }

        throw new UnsupportedOperationException("不支持的方法 " + method);
    }
}
