package com.yunke.backend.workspace.exception;

/**
 * 成员配额超限异常 - 完全按照AFFiNE实现
 * 对应: AFFiNE中的成员配额超限错误处理
 */
public class MemberQuotaExceededException extends RuntimeException {

    public MemberQuotaExceededException() {
        super("Member quota exceeded");
    }

    public MemberQuotaExceededException(String message) {
        super(message);
    }

    public MemberQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}