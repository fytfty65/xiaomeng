package com.omniroute.app.core.network.dto;

/** 一句话结构化解析请求。 */
public final class IntentParseRequest {

    private final String text;

    public IntentParseRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
