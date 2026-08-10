package com.pta.outsourcing.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("performance_record")
public class PerformanceRecord {

    private Long id;
    private Long evaluatorUserId;
    private Long evaluatedUserId;
    private Long projectId;
    private String periodType;
    private String periodValue;
    private String grade;
    private String comment;
    @TableField("is_current")
    private Boolean current;
    private String modificationReason;
    private LocalDateTime effectiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
