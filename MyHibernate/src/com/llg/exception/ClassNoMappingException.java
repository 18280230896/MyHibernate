package com.llg.exception;

/**
 * 当一个类没有被映射到具体的表时会抛出次异常
 */
public class ClassNoMappingException extends RuntimeException {
    public static final String ERR_MSG = "类没有被映射到具体的表";

    public ClassNoMappingException() {
    }

    public ClassNoMappingException(String message) {
        super(message);
    }
}
