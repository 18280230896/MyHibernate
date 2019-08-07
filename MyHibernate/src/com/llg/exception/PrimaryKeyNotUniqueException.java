package com.llg.exception;

/**
 * 当一个类所映射的主键列不唯一时会抛出次异常
 */
public class PrimaryKeyNotUniqueException extends RuntimeException {
    public static final String ERR_MSG = "类所映射的主键列不唯一";

    public PrimaryKeyNotUniqueException() {
    }
    public PrimaryKeyNotUniqueException(String message) {
        super(message);
    }
}
