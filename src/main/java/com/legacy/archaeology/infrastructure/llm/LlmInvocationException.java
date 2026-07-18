package com.legacy.archaeology.infrastructure.llm;

/** LLM呼び出し失敗例外 */
public class LlmInvocationException extends RuntimeException {

    public LlmInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
