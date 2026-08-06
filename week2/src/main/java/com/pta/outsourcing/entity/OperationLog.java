package com.pta.outsourcing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("operation_log")
public class OperationLog {

    private Long id;
    private Long operatorId;
    private String operatorName;
    private String moduleName;
    private String operationType;
    private String requestPath;
    private String requestParams;
    private String result;
    private String errorMessage;
    private LocalDateTime createdAt;
}
