package com.pta.outsourcing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("notification_message")
public class NotificationMessage {

    private Long id;
    private String eventId;
    private Long applicationId;
    private Long recipientId;
    private String eventType;
    private String title;
    private String content;
    private String status;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
