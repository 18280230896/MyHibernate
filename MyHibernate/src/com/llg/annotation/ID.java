package com.llg.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 此注解用于标记一个字段，用与表示此字段对应表中的唯一标识
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ID {

    /**
     * 表中唯一标识列的名称
     * @return
     */
    String name() default "id";
}
