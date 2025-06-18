package com.starlwr.bot.adapter.napcat.service;

import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.adapter.napcat.annotation.NapCatHttpApi;

/**
 * StarBot NapCat HTTP 服务接口
 */
public interface StarBotNapCatHttpService {
    @NapCatHttpApi(name = "获取版本信息", url = "/get_version_info")
    JSONObject getVersionInfo(JSONObject params);

    @NapCatHttpApi(name = "发送私聊消息", url = "/send_private_msg")
    JSONObject sendPrivateMsg(JSONObject params);

    @NapCatHttpApi(name = "发送群聊消息", url = "/send_group_msg")
    JSONObject sendGroupMsg(JSONObject params);
}
