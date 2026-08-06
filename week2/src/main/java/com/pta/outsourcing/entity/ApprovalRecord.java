package com.pta.outsourcing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("approval_record")
public class ApprovalRecord {

    private Long id;
    private Long applicationId;
    private Long approverId;
    private String result;
    private String opinion;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
