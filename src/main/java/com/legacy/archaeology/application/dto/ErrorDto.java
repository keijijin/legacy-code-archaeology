package com.legacy.archaeology.application.dto;

import lombok.Builder;
import lombok.Getter;

/** API エラーレスポンス DTO */
@Getter
@Builder
public class ErrorDto {
    private String errorCode;
    private String message;
    private String traceId;
}
