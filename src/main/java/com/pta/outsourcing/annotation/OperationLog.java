package com.pta.outsourcing.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * 业务模块名称，用于操作日志筛选。
     *
     * @return 模块名称。
     */
    String moduleName();

    /**
     * 操作类型，用于描述当前接口执行的业务动作。
     *
     * @return 操作类型。
     */
    String operationType();
}
