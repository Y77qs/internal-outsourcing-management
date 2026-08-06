package com.pta.outsourcing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("onboarding_application")
public class OnboardingApplication {

    private Long id;
    private Long applicantId;
    private Long departmentId;
    private Long projectId;
    private String positionType;
    private String applicationReason;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
