package com.omniroute.app.core.network.dto;

/** 携带后端错误文案的异常，UI 直接展示其 message。 */
public final class ApiException extends Exception {

    public ApiException(String message) {
        super(message);
    }
}
