package com.llg.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 次注解用于标记一个字段，用于映射该字段所对应的数据库表的列
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    /**
     * 表示该字段所对应的数据库表的列名称
     * @return
     */
    String value();
}
