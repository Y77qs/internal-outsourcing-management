package com.pta.outsourcing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_department")
public class SysDepartment {

    private Long id;
    private Long parentId;
    private String departmentCode;
    private String departmentName;
    private Long leaderUserId;
    private String description;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
