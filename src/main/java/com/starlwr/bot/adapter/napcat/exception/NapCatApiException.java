package com.starlwr.bot.adapter.napcat.exception;

import com.alibaba.fastjson2.JSONObject;

/**
 * NapCat 接口异常
 */
public class NapCatApiException extends RuntimeException {
    private final String api;

    private final JSONObject params;

    private final int code;

    private final String message;

    public NapCatApiException(String api, JSONObject params, Integer code, String message) {
        super();
        this.api = api;
        this.params = params;
        this.code = code;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return "NapCat API 请求异常, 接口: " + api + ", 请求参数: " + params.toJSONString() + ", 错误码: " + code + ", 信息: " + message;
    }
}
