package com.starlwr.bot.adapter.napcat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误代码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {
    SUCCESS(0, "成功"),
    UNKNOWN(1, "未知异常"),
    API_ERROR(2, "NapCat API 返回错误代码"),
    UNKNOWN_TARGET_TYPE(3, "未知的推送目标类型");

    private final int code;
    private final String msg;
}
