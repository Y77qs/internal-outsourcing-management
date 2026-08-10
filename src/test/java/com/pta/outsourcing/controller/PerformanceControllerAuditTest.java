package com.pta.outsourcing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.pta.outsourcing.annotation.OperationLog;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class PerformanceControllerAuditTest {

    @Test
    void shouldAuditPerformanceUserOptionSearch() throws NoSuchMethodException {
        Method method = PerformanceController.class.getDeclaredMethod(
                "searchUserOptions", String.class, Long.class);

        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        assertThat(operationLog).isNotNull();
        assertThat(operationLog.moduleName()).isEqualTo("绩效管理");
        assertThat(operationLog.operationType()).isEqualTo("搜索绩效人员");
    }
}
