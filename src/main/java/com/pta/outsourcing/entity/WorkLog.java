package com.pta.outsourcing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("work_log")
public class WorkLog {

    private Long id;
    private Long userId;
    private Long projectId;
    private LocalDate workDate;
    private String workContent;
    private String issueRecord;
    private String completionStatus;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
