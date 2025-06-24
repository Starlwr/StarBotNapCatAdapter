package com.starlwr.bot.adapter.napcat.exception;

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;

/**
 * NapCat 接口异常
 */
@Getter
public class NapCatApiException extends RuntimeException {
    private final String api;

    private final JSONObject params;

    private final int code;

    private final String msg;

    public NapCatApiException(String api, JSONObject params, Integer code, String msg) {
        super();
        this.api = api;
        this.params = params;
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String getMessage() {
        return "NapCat API 请求异常, 接口: " + api + ", 请求参数: " + params.toJSONString() + ", 错误码: " + code + ", 信息: " + msg;
    }
}
