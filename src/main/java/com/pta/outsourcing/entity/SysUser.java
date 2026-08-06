package com.pta.outsourcing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_user")
public class SysUser {

    private Long id;
    private String username;
    private String passwordHash;
    private String phone;
    private String email;
    private String realName;
    private Long departmentId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
