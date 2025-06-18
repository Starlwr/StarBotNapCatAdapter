package com.starlwr.bot.adapter.napcat.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * NapCat HTTP 接口注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NapCatHttpApi {
    /**
     * 接口名称
     */
    String name();

    /**
     * 接口地址
     */
    String url();
}
