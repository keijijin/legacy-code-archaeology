package com.legacy.archaeology.presentation.api;

import com.legacy.archaeology.application.dto.ErrorDto;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** グローバル例外ハンドラ。業務エラーとシステムエラーを分離する。 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleBusinessError(IllegalArgumentException ex) {
        log.warn("業務エラー: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ErrorDto.builder()
                                .errorCode("BUSINESS_ERROR")
                                .message(ex.getMessage())
                                .traceId(MDC.get("traceId"))
                                .build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorDto> handleConflictError(IllegalStateException ex) {
        log.warn("競合エラー: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ErrorDto.builder()
                                .errorCode("CONFLICT_ERROR")
                                .message(ex.getMessage())
                                .traceId(MDC.get("traceId"))
                                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleValidationError(MethodArgumentNotValidException ex) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> e.getField() + ": " + e.getDefaultMessage())
                        .findFirst()
                        .orElse("入力値が不正です");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ErrorDto.builder()
                                .errorCode("VALIDATION_ERROR")
                                .message(message)
                                .traceId(MDC.get("traceId"))
                                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleSystemError(Exception ex) {
        log.error("システムエラー", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ErrorDto.builder()
                                .errorCode("SYSTEM_ERROR")
                                .message("内部エラーが発生しました")
                                .traceId(MDC.get("traceId"))
                                .build());
    }
}
