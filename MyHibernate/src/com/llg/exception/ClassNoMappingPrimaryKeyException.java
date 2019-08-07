package com.llg.exception;

/**
 * 当一个类没有属性映射到表的主键时会抛出此异常
 */
public class ClassNoMappingPrimaryKeyException extends RuntimeException {
    public static final String ERR_MSG = "类没有属性映射到表的主键";

    public ClassNoMappingPrimaryKeyException() {
    }

    public ClassNoMappingPrimaryKeyException(String message) {
        super(message);
    }
}
